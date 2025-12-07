package io.knifer.freebox.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.M3u8AdFilterHandler;
import io.knifer.freebox.model.domain.M3u8AdFilterResult;
import io.knifer.freebox.util.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * m3u8广告过滤器，会尝试过滤m3u8中的异常（广告）分段，并把文件中可能存在的子播放列表提取、合并成一个播放列表
 * 会智能选择过滤方式：
 * 1.数字序号过滤：
 *   若发现ts片段以数字命名，且数字有递增趋势，则进行数字序号过滤；
 *   会过滤掉数字序号不连续的ts片段，并删除这类ts片段前一行的DISCONTINUITY标签
 * 2.命名长度过滤（与数字序号过滤同时进行）：
 *   若发现ts片段命名长度不一致，则将其过滤，并删除这类ts片段前一行的DISCONTINUITY标签
 * 3.正态分布暴力过滤：
 *   如果前两个过滤逻辑都未成功进行，则进行正态分布暴力过滤；
 *   通过DISCONTINUITY标签，把m3u8内容分割为一个个分段，若发现分段数大于minSegmentCountForStats，说明可以进行暴力过滤；
 *   通过正态分布算法，过滤掉ts片段数明显低于其他分段的异常（广告）分段，分段数越多，过滤效果越理想。但若各分段之间ts片段数相似，则容易遗漏、误删
 */
@Slf4j
@Data
public class SmartM3u8AdFilterHandler implements M3u8AdFilterHandler {

    /**
     * 最小行数，小于此值不会触发任何过滤
     */
    private int minLineCountForFilter = 30;
    /**
     * 数字序号过滤 & 命名长度过滤 - 针对两种ts类型分别会尝试取样识别3次，其中只有识别成功次数达到此值时，才认为识别成功，可以采用对应的过滤方式
     */
    private int needValidRecognizeCount = 2;
    /**
     * 数字序号过滤 & 命名长度过滤 - 每次识别的取样行数
     */
    private int recognizeSampleLineCount = 12;
    /**
     * 数字序号过滤 & 命名长度过滤 - 允许的特征最大偏差次数，越低越容易判定为广告
     */
    private int allowedMaxDeviationCount = 1;
    /**
     * 命名长度过滤 - #EXITINF长度基准值，只有连续出现相同长度的#EXITINF数量等于这个值，将其确定为正常ts片段的特征，并据此过滤异常ts片段
     */
    private int extInfBenchmarkNum = 5;
    /**
     * 正态分布暴力过滤 - 默认的最小正常ts片段数量，若计算ts片段数量失败，使用该值
     */
    private int minNormalSegmentCount = 7;
    /**
     * 正态分布暴力过滤 - 动态阈值因子，用于动态计算阈值，阈值越小，过滤效果越强，但越容易误删
     */
    private double dynamicThresholdFactor = 0.3;
    /**
     * 正态分布暴力过滤 - 最小分段数，用于判断是否执行过滤
     */
    private int minSegmentCountForStats = 3;
    /**
     * 下载子播放列表文件的超时时间
     */
    private int downloadTimeout = 6;

    /**
     * 是否强制进行分段过滤
     */
    private boolean doForceSegmentFilterFlag = false;

    private static final String[] TAG_PREFIX_WITH_TS_LINE = {
            "#EXTINF",
            "#EXT-X-DISCONTINUITY",
            "#EXT-X-BYTERANGE",
            "#EXT-X-PROGRAM-DATE-TIME"
    };

    public static final String EXTRA_KEY_DTF = "dynamicThresholdFactor";

    /**
     * 主处理方法 - 直接返回处理后的m3u8内容字符串
     */
    @Override
    public M3u8AdFilterResult handle(
            String url, String content, Map<String, Object> extraData
    ) {
        List<String> lines;

        init(extraData);
        lines = List.of(content.split("\n"));
        if (isMasterPlaylist(lines)) {
            log.info("start process master play list");
            return processMasterPlaylist(url, content);
        } else {
            log.info("start process media play list");
            return processSinglePlayList(url, content);
        }
    }

    private void init(Map<String, Object> extraData) {
        Double dynamicThresholdParam;

        dynamicThresholdParam = MapUtil.getDouble(extraData, EXTRA_KEY_DTF, null);
        if (dynamicThresholdParam == null || dynamicThresholdParam <= 0) {

            return;
        }
        dynamicThresholdFactor = dynamicThresholdParam;
        doForceSegmentFilterFlag = true;
    }

    /**
     * 处理主播放列表并返回合并后的内容
     */
    private M3u8AdFilterResult processMasterPlaylist(String masterUrl, String masterContent) {
        List<String> lines = List.of(masterContent.split(StrPool.LF));
        String baseUrl = getBaseUrl(masterUrl);
        List<String> subPlaylistUrls = extractSubPlaylistUrls(lines, baseUrl);
        List<String> filteredSubPlaylistContents = new ArrayList<>(subPlaylistUrls.size());
        String subPlaylistContent;
        Pair<Integer, List<String>> adLineCountAndLinesFiltered;
        int adLineCount = 0;
        String subPlaylistFullUrl;

        for (String subPlaylistUrl : subPlaylistUrls) {
            log.info("process subPlaylist, subPlaylistUrl={}", subPlaylistUrl);
            try {
                subPlaylistFullUrl = resolveRelativeUrl(subPlaylistUrl, baseUrl);
                subPlaylistContent = HttpUtil.getAsync(
                        subPlaylistFullUrl, HttpHeaders.USER_AGENT, BaseValues.USER_AGENT
                ).get(downloadTimeout, TimeUnit.SECONDS);
                if (StringUtils.isBlank(subPlaylistContent)) {
                    log.warn("process subPlaylist failed, subPlaylistUrl={}, content is blank", subPlaylistUrl);
                    continue;
                }
                adLineCountAndLinesFiltered =
                        doFilter(StrUtil.split(subPlaylistContent, StrPool.LF), getBaseUrl(subPlaylistFullUrl));
                adLineCount += adLineCountAndLinesFiltered.getLeft();
                filteredSubPlaylistContents.add(StringUtils.join(adLineCountAndLinesFiltered.getRight(), StrPool.LF));
            } catch (Exception e) {
                log.error("process subPlaylist failed, subPlaylistUrl={}", subPlaylistUrl, e);
            }
        }

        return filteredSubPlaylistContents.isEmpty() ?
                M3u8AdFilterResult.of(0, masterContent) :
                M3u8AdFilterResult.of(adLineCount, mergeSubPlaylists(filteredSubPlaylistContents));
    }

    /**
     * 处理媒体播放列表
     */
    private M3u8AdFilterResult processSinglePlayList(String mediaPlaylistUrl, String content) {
        List<String> lines = List.of(content.split(StrPool.LF));
        Pair<Integer, List<String>> adLineCountAndLinesFiltered = doFilter(lines, getBaseUrl(mediaPlaylistUrl));

        return M3u8AdFilterResult.of(
                adLineCountAndLinesFiltered.getLeft(),
                StringUtils.join(adLineCountAndLinesFiltered.getRight(), StrPool.C_LF)
        );
    }

    /**
     * 合并子播放列表为一个大的播放列表
     */
    private String mergeSubPlaylists(List<String> filteredSubPlaylistContents) {
        List<String> mergedLines = new ArrayList<>();
        boolean isFirstPlaylist = true;
        double maxTargetDuration = calculateMaxTargetDuration(filteredSubPlaylistContents);
        String lastLine;

        // 添加基本头部信息
        mergedLines.add("#EXTM3U");
        mergedLines.add("#EXT-X-VERSION:3");
        if (maxTargetDuration > 0) {
            mergedLines.add("#EXT-X-TARGETDURATION:" + (int) Math.ceil(maxTargetDuration));
        }
        mergedLines.add("#EXT-X-MEDIA-SEQUENCE:0");

        // 合并每个子播放列表的内容
        for (String content : filteredSubPlaylistContents) {
            List<String> lines = List.of(content.split("\n"));
            boolean inHeader = true;

            for (String line : lines) {
                // 跳过子播放列表的头部信息（除了第一个播放列表）
                if (inHeader) {
                    if (line.startsWith("#EXTINF:") || (!line.startsWith("#") && line.contains(".ts"))) {
                        inHeader = false;
                    } else {
                        // 第一个播放列表保留部分关键头部信息
                        if (isFirstPlaylist && (
                                line.startsWith("#EXT-X-KEY:") ||
                                        line.startsWith("#EXT-X-MAP:") ||
                                        line.startsWith("#EXT-X-PLAYLIST-TYPE:")
                        )) {
                            mergedLines.add(line);
                        }
                        continue;
                    }
                }
                // 添加内容行
                if (!isFirstPlaylist && line.startsWith("#EXTINF:")) {
                    // 在非第一个播放列表的片段前添加DISCONTINUITY标记
                    mergedLines.add("#EXT-X-DISCONTINUITY");
                }
                mergedLines.add(line);
            }
            isFirstPlaylist = false;
        }

        // 添加结束标记
        lastLine = CollUtil.getLast(mergedLines);
        if (lastLine != null && !lastLine.startsWith("#EXT-X-ENDLIST")) {
            mergedLines.add("#EXT-X-ENDLIST");
        }

        return StringUtils.join(mergedLines, StrPool.LF);
    }

    private static double calculateMaxTargetDuration(List<String> filteredSubPlaylistContents) {
        double maxTargetDuration = 0;

        // 收集所有必要的头部信息
        for (String content : filteredSubPlaylistContents) {
            List<String> lines = List.of(content.split("\n"));
            for (String line : lines) {
                if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                    try {
                        double duration = Double.parseDouble(line.substring("#EXT-X-TARGETDURATION:".length()));
                        maxTargetDuration = Math.max(maxTargetDuration, duration);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return maxTargetDuration;
    }

    /**
     * 判断是否是主播放列表
     */
    private boolean isMasterPlaylist(List<String> lines) {
        return lines.stream().anyMatch(line -> line.startsWith("#EXT-X-STREAM-INF:"));
    }

    /**
     * 从主播放列表中提取子m3u8URL
     */
    private List<String> extractSubPlaylistUrls(List<String> lines, String baseUrl) {
        List<String> urls = new ArrayList<>();
        String line;
        String urlLine;
        String fullUrl;

        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i);
            if (line.startsWith("#EXT-X-STREAM-INF:") && i + 1 < lines.size()) {
                urlLine = lines.get(i + 1);
                if (!urlLine.startsWith("#")) {
                    fullUrl = resolveRelativeUrl(urlLine, baseUrl);
                    urls.add(fullUrl);
                }
            }
        }

        return urls;
    }

    /**
     * 计算动态阈值
     */
    private int calculateDynamicThreshold(List<Segment> segments) {
        // 收集所有片段的TS数量（排除第一个片段，因为它包含头部信息）
        List<Integer> tsCounts = new ArrayList<>();
        for (int i = 1; i < segments.size(); i++) {
            tsCounts.add(segments.get(i).getTsCount());
        }

        if (tsCounts.isEmpty()) {
            return minNormalSegmentCount;
        }

        // 计算平均值
        double average = tsCounts.stream().mapToInt(Integer::intValue).average().orElse(minNormalSegmentCount);

        // 计算标准差
        double variance = 0;
        for (int count : tsCounts) {
            variance += Math.pow(count - average, 2);
        }
        double stdDev = Math.sqrt(variance / tsCounts.size());

        // 动态阈值 = 平均值 - (标准差 * 因子)
        int dynamicThreshold = (int) Math.max(3, average - (stdDev * dynamicThresholdFactor));

        // 确保动态阈值不会太大或太小
        dynamicThreshold = Math.max(3, Math.min(dynamicThreshold, minNormalSegmentCount * 2));

        log.info("dynamicThresholdFactor = {}", dynamicThresholdFactor);
        log.info("average = {}", String.format("%.2f", average));
        log.info("stdDev = {}", String.format("%.2f", stdDev));
        log.info("dynamicThreshold = {}", dynamicThreshold);

        return dynamicThreshold;
    }

    /**
     * 尝试通过序号、命名长度的方式按行过滤
     * 如果无法过滤，则将m3u8内容分割成多个片段，用于正态分布暴力过滤
     * @param lines m3u8内容
     * @param baseUrl m3u8文件地址，用于拼接绝对路径
     * @return pair, left=是否直接返回过滤结果，right=按行过滤结果（String）或分割片段结果（Segment列表）
     */
    private Pair<Integer, List<String>> doFilter(List<String> lines, String baseUrl) {
        int lineSize;
        int recognizeSuccessCount = 0;
        int benchmarkLineIdx = -1;
        int randomStartLineIdx;
        Pair<Integer, List<String>> result;
        Integer benchmarkTsNameLen;

        lineSize = lines.size();
        if (lineSize < minLineCountForFilter) {
            log.info("lineSize < {}, skip filter", minLineCountForFilter);

            return Pair.of(
                    0,
                    lines.stream()
                            .map(l -> StringUtils.isNotBlank(l) && StringUtils.startsWith(l, "#") ?
                                    l : resolveRelativeUrl(l, baseUrl)
                            )
                            .toList()
            );
        }
        // 方式1：识别是否是数字递增型，取样3次：开头、中间随机、结尾
        if (isDigitTsType(lines, 0, recognizeSampleLineCount)) {
            recognizeSuccessCount++;
            benchmarkLineIdx = 0;
        }
        randomStartLineIdx = RandomUtils.nextInt(recognizeSampleLineCount, lineSize - recognizeSampleLineCount * 2 + 1);
        if (isDigitTsType(lines, randomStartLineIdx, randomStartLineIdx + recognizeSampleLineCount)) {
            recognizeSuccessCount++;
            benchmarkLineIdx = randomStartLineIdx;
        }
        if (isDigitTsType(lines, lineSize - recognizeSampleLineCount, lineSize)) {
            recognizeSuccessCount++;
            benchmarkLineIdx = lineSize - recognizeSampleLineCount;
        }
        if (recognizeSuccessCount >= needValidRecognizeCount) {
            result = doDigitFilter(lines, benchmarkLineIdx, baseUrl);
            if (result.getLeft() > 0) {

                return result;
            }
        }
        // 方式2：识别是否是命名长度型，如果是，返回基准命名长度
        benchmarkTsNameLen = validHashTypeAndGetBenchmarkTsNameLen(lines);
        if (benchmarkTsNameLen != null) {
            result = doHashFilter(lines, benchmarkTsNameLen, baseUrl);
            if (result.getLeft() > 0) {

                return result;
            }
        }
        // 方式1、方式2不适用，或者适用但未能成功过滤任何广告时，考虑基于正态分布算法进行强制过滤
        if (doForceSegmentFilterFlag) {

            return doForceFilter(lines, baseUrl);
        } else {
            // 禁止了强制过滤
            log.info("force filter is disabled, return origin lines");

            return Pair.of(0, lines);
        }
    }

    /**
     * 是否是数字ts类型
     * @param lines m3u8行
     * @param startLineIdx 开始行索引
     * @param endLineIdx 结束行索引
     * @return boolean
     */
    private boolean isDigitTsType(List<String> lines, int startLineIdx, int endLineIdx) {
        String line;
        int totalTsCount = 0;
        Long tsDigitName;
        Long lastTsDigitName = null;
        int diffNum = 0;

        for (int i = startLineIdx; i < endLineIdx; i++) {
            line = lines.get(i);
            if (StringUtils.isEmpty(line) || line.startsWith("#") || !line.endsWith(".ts")) {
                continue;
            }
            totalTsCount++;
            tsDigitName = extractNumberBeforeTs(line);
            if (tsDigitName == null) {
                if (++diffNum > allowedMaxDeviationCount) {

                    return false;
                }
                continue;
            }
            if (lastTsDigitName == null) {
                lastTsDigitName = tsDigitName;
                continue;
            }
            if (tsDigitName.equals(lastTsDigitName + 1)) {
                lastTsDigitName = tsDigitName;
            } else {
                if (++diffNum > allowedMaxDeviationCount) {

                    return false;
                }
            }
        }

        return totalTsCount > 1;
    }


    /**
     * 判断ts是否是命名长度类型并返回基准命名长度
     * 共取样3次：开头、中间随机、结尾，然后拿到汇总结果
     * @param lines m3u8行
     * @return 基准命名长度。可依据这个进行广告过滤，如果判断失败，返回null
     */
    @Nullable
    private Integer validHashTypeAndGetBenchmarkTsNameLen(List<String> lines) {
        int diffCount = 0;
        int lineSize = lines.size();
        int randomStartLineIdx;
        Integer benchmarkNameLen;
        Map<Integer, Integer> nameLenAndCountMap = new HashMap<>();
        Integer benchmarkNameLenInMap;

        benchmarkNameLen= validHashTypeAndGetBenchmarkTsNameLen(lines, 0, recognizeSampleLineCount);
        if (benchmarkNameLen == null && ++diffCount > allowedMaxDeviationCount) {

            return null;
        } else {
            nameLenAndCountMap.put(benchmarkNameLen, 1);
        }
        randomStartLineIdx = RandomUtils.nextInt(recognizeSampleLineCount, lineSize - recognizeSampleLineCount * 2 + 1);
        benchmarkNameLen = validHashTypeAndGetBenchmarkTsNameLen(
                lines, randomStartLineIdx, randomStartLineIdx + recognizeSampleLineCount
        );
        if (benchmarkNameLen == null && ++diffCount > allowedMaxDeviationCount) {

            return null;
        } else {
            benchmarkNameLenInMap = nameLenAndCountMap.getOrDefault(benchmarkNameLen, 0);
            nameLenAndCountMap.put(benchmarkNameLen, benchmarkNameLenInMap + 1);
        }
        benchmarkNameLen = validHashTypeAndGetBenchmarkTsNameLen(
                lines, lineSize - recognizeSampleLineCount, lineSize
        );
        if (benchmarkNameLen == null && ++diffCount > allowedMaxDeviationCount) {

            return null;
        } else {
            benchmarkNameLenInMap = nameLenAndCountMap.getOrDefault(benchmarkNameLen, 0);
            nameLenAndCountMap.put(benchmarkNameLen, benchmarkNameLenInMap + 1);
        }

        return nameLenAndCountMap.size() > allowedMaxDeviationCount ?
                null : nameLenAndCountMap.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 取样判断ts是否是命名长度类型并返回基准命名长度
     * @param lines m3u8行
     * @param startLineIdx 开始索引
     * @param endLineIdx 结束索引
     * @return 基准命名长度。可依据这个进行广告过滤，如果判断失败，返回null
     */
    @Nullable
    private Integer validHashTypeAndGetBenchmarkTsNameLen(List<String> lines, int startLineIdx, int endLineIdx) {
        String line;
        int totalTsCount = 0;
        int tsNameLen;
        Map<Integer, Integer> nameLenAndCountMap = new HashMap<>();
        int tsNameLenCount;
        int benchmarkTsNameLen;
        int benchmarkTsNameLenCount;
        int key, val;
        int diffCount;

        for (int i = startLineIdx; i < endLineIdx; i++) {
            line = lines.get(i);
            if (StringUtils.isEmpty(line) || line.startsWith("#") || !line.endsWith(".ts")) {
                continue;
            }
            tsNameLen = line.indexOf(".ts");
            if (tsNameLen == 0) {
                continue;
            }
            totalTsCount++;
            tsNameLenCount = nameLenAndCountMap.getOrDefault(tsNameLen, 0);
            nameLenAndCountMap.put(tsNameLen, tsNameLenCount + 1);
        }
        if (totalTsCount < 2 || nameLenAndCountMap.size() > 1 + allowedMaxDeviationCount) {

            return null;
        }
        benchmarkTsNameLen = -1;
        benchmarkTsNameLenCount = -1;
        diffCount = 0;
        for (Map.Entry<Integer, Integer> nameLenAndCount : nameLenAndCountMap.entrySet()) {
            if (benchmarkTsNameLen == -1) {
                benchmarkTsNameLen = nameLenAndCount.getKey();
                benchmarkTsNameLenCount = nameLenAndCount.getValue();
                continue;
            }
            key = nameLenAndCount.getKey();
            val = nameLenAndCount.getValue();
            if (benchmarkTsNameLen != key) {
                if (val > benchmarkTsNameLenCount) {
                    diffCount += benchmarkTsNameLenCount;
                    benchmarkTsNameLen = key;
                    benchmarkTsNameLenCount = val;
                } else {
                    diffCount += val;
                }
                if (diffCount > allowedMaxDeviationCount) {

                    return null;
                }
            }
        }

        return benchmarkTsNameLen;
    }

    /**
     * 数字递增ts过滤
     * @param lines m3u8行
     * @param benchmarkLineIdx 开始索引，会通过该索引获取基准ts名称
     * @param baseUrl 基础url
     * @return 过滤ts文件数和过滤后的行
     */
    private Pair<Integer, List<String>> doDigitFilter(
            List<String> lines,
            int benchmarkLineIdx,
            String baseUrl
    ) {
        int lineSize = lines.size();
        Pair<Integer, List<String>> result0;
        Pair<Integer, List<String>> result1;
        Long benchmarkTsDigitName;

        log.info("digit filter, start benchmark line: {}", benchmarkLineIdx + 1);
        benchmarkTsDigitName = getBenchmarkTsDigitName(lines, benchmarkLineIdx);
        if (benchmarkLineIdx == 0) {

            return doDigitFilter(lines, benchmarkTsDigitName, benchmarkLineIdx, baseUrl, false);
        } else if (benchmarkLineIdx == lineSize - 1) {

            return doDigitFilter(lines, benchmarkTsDigitName, benchmarkLineIdx, baseUrl, true);
        } else {
            result0 = doDigitFilter(lines, benchmarkTsDigitName, benchmarkLineIdx, baseUrl, true);
            result1 = doDigitFilter(lines, benchmarkTsDigitName, benchmarkLineIdx + 1, baseUrl, false);

            return Pair.of(
                    result0.getLeft() + result1.getLeft(),
                    Stream.concat(result0.getRight().stream(), result1.getRight().stream()).toList()
            );
        }
    }

    /**
     * 数字递增ts过滤
     * 方法始终保证startIdx会进入判断逻辑，因此连续调用本方法时要注意startIdx要+1，否则startIdx这行数据会出现重复
     * @param lines m3u8行
     * @param benchmarkTsDigitName 基准ts名称
     * @param startIdx 开始索引
     * @param baseUrl 基础url，用于拼接绝对路径
     * @param reverseFlag 是否倒序进行。倒序是从startIdx往0遍历，正序是从startIdx往lineSize-1遍历
     * @return 过滤ts文件数和过滤后的行
     */
    private Pair<Integer, List<String>> doDigitFilter(
            List<String> lines, Long benchmarkTsDigitName, int startIdx, String baseUrl, boolean reverseFlag
    ) {
        int lineSize = lines.size();
        int endIdx;
        int stepVal;
        String line;
        Long tsDigitName;
        List<String> result = new ArrayList<>(lineSize);
        int jumpStep;
        String lineWithTs;
        int adLineCount = 0;

        if (reverseFlag) {
            endIdx = -1;
            stepVal = -1;
        } else {
            endIdx = lineSize;
            stepVal = 1;
        }
        for (int i = startIdx; i != endIdx; i += stepVal) {
            line = lines.get(i);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (line.startsWith("#") || !line.endsWith(".ts")) {
                result.add(line);
                continue;
            }
            tsDigitName = extractNumberBeforeTs(line);
            if (
                    benchmarkTsDigitName.equals(tsDigitName) ||
                    Long.valueOf(benchmarkTsDigitName + stepVal).equals(tsDigitName)
            ) {
                benchmarkTsDigitName = tsDigitName;
                line = resolveRelativeUrl(line, baseUrl);
                result.add(line);
            } else {
                log.info("digit, filter line {}:\n{}", i + 1, line);
                if (reverseFlag) {
                    jumpStep = 0;
                    for (int j = i - 1; j >= 0; j--) {
                        lineWithTs = lines.get(j);
                        if (StringUtils.startsWithAny(lineWithTs, TAG_PREFIX_WITH_TS_LINE)) {
                            jumpStep--;
                            log.info("digit, filter line {}:\n{}", j + 1, lineWithTs);
                        } else {
                            break;
                        }
                    }
                    i += jumpStep;
                    adLineCount = adLineCount - jumpStep + 1;
                } else {
                    adLineCount = adLineCount + 1 + filterBackwardLinesForTs(result, i + 1);
                }
            }
        }
        if (reverseFlag) {
            Collections.reverse(result);
        }

        return Pair.of(adLineCount, result);
    }

    private long getBenchmarkTsDigitName(List<String> lines, int startIdx) {
        int lineSize = lines.size();
        String line;
        Long result;

        for (int i = startIdx; i < lineSize; i++) {
            line = lines.get(i);
            if (StringUtils.isBlank(line) || line.startsWith("#") || !line.endsWith(".ts")) {
                continue;
            }
            result = extractNumberBeforeTs(line);
            if (result == null) {
                continue;
            }

            return result;
        }
        throw new FBException("failed, algorithm error");
    }

    /**
     * 根据给定的ts基准命名长度，进行命名长度过滤
     * @param lines m3u8行
     * @param benchmarkTsNameLen 基准命名长度
     * @param baseUrl url，用于拼接绝对路径
     * @return pair, left=过滤的广告数量，right=过滤后的行
     */
    private Pair<Integer, List<String>> doHashFilter(List<String> lines, int benchmarkTsNameLen, String baseUrl) {
        int lineSize = lines.size();
        String line;
        int tsNameLen;
        List<String> result = new ArrayList<>(lineSize);
        int adLineCount = 0;

        for (int i = 0; i < lineSize; i++) {
            line = lines.get(i);
            if (StringUtils.isBlank(line)) {
                continue;
            }
            if (line.startsWith("#") || !line.endsWith(".ts")) {
                result.add(line);
                continue;
            }
            tsNameLen = line.indexOf(".ts");
            if (tsNameLen == 0) {
                continue;
            }
            if (tsNameLen != benchmarkTsNameLen) {
                log.info("hash, filter line {}:\n{}", i + 1, line);
                adLineCount = adLineCount + 1 + filterBackwardLinesForTs(result, i + 1);
                continue;
            }
            line = resolveRelativeUrl(line, baseUrl);
            result.add(line);
        }

        return Pair.of(adLineCount, result);
    }

    /**
     * 回溯过滤，寻找与ts行有关的标签行，将其移除出列表
     * TODO 有一个小缺陷：此方法只是回溯过滤ts行之前的相关标签，ts行之后的相关标签会残留，但目前影响不大，待后续优化
     * @param backwardLines 待过滤的行
     * @param tsLineNo ts行号，用于日志打印
     * @return 过滤的广告行数
     */
    private int filterBackwardLinesForTs(List<String> backwardLines, int tsLineNo) {
        ListIterator<String> prevIter = backwardLines.listIterator(backwardLines.size());
        String prevLine;
        int adLineCount = 0;
        int prevCount = 0;

        while (prevIter.hasPrevious()) {
            prevLine = prevIter.previous();
            prevCount++;
            if (StringUtils.startsWithAny(prevLine, TAG_PREFIX_WITH_TS_LINE)) {
                prevIter.remove();
                adLineCount++;
                log.info(
                        "digit, filter line {}:\n{}",
                        tsLineNo - prevCount, prevLine
                );
            } else {
                break;
            }
        }

        return adLineCount;
    }

    /**
     * 提取.ts前的数字名称（移除数字外的字符）
     * @param line m3u8行
     * @return pair, left=过滤的广告数量，right=过滤后的行
     */
    @Nullable
    private Long extractNumberBeforeTs(String line) {
        String fullName = StringUtils.substringAfterLast(line, StrPool.SLASH);
        String name;
        int nameLen;

        if (fullName.isEmpty()) {
            fullName = line;
        }
        name = StringUtils.getDigits(fullName);
        if (name.isEmpty()) {

            return null;
        }
        nameLen = name.length();
        try {
            return Long.parseLong(nameLen > 19 ? name.substring(nameLen - 19, nameLen) : name);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 正态分布强制过滤
     * @param lines m3u8行
     * @param baseUrl url，用于拼接绝对路径
     * @return pair, left=过滤的广告数量，right=过滤后的行
     */
    private Pair<Integer, List<String>> doForceFilter(List<String> lines, String baseUrl) {
        int lineSize = lines.size();
        String line;
        List<Segment> segments = new ArrayList<>();
        List<String> currentSegmentLines = new ArrayList<>();
        boolean inFirstSegment = true;
        List<String> result = new ArrayList<>();
        int dynamicThreshold;
        int adLineCount = 0;

        for (int i = 0; i < lineSize; i++) {
            line = lines.get(i);
            // 当遇到DISCONTINUITY时，开始新的片段
            if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                // 保存当前片段（如果有内容）
                if (!currentSegmentLines.isEmpty()) {
                    segments.add(new Segment(
                            i + 1 - currentSegmentLines.size(), currentSegmentLines, inFirstSegment
                    ));
                    currentSegmentLines.clear();
                    inFirstSegment = false;
                }
            }
            currentSegmentLines.add(line);
        }
        // 添加最后一个片段
        if (!currentSegmentLines.isEmpty()) {
            segments.add(new Segment(
                    lineSize - currentSegmentLines.size() + 1,
                    currentSegmentLines,
                    inFirstSegment
            ));
        }
        if (segments.size() < minSegmentCountForStats) {
            // 片段数量过少，不进行过滤
            log.info("segment count is less than minSegmentCountForStats, skip force filter");

            return Pair.of(0, lines);
        }
        // 开始过滤片段，先计算动态阈值
        dynamicThreshold = calculateDynamicThreshold(segments);
        log.info("use dynamicThreshold: {}", dynamicThreshold);
        for (Segment segment : segments) {
            if (isAdSegment(segment, dynamicThreshold)) {
                log.info(
                        "filter ad segment, start line no: {}, ts count: {}",
                        segment.getStartLineNo(), segment.getTsCount()
                );
                adLineCount++;
            } else {
                for (String l : segment.getLines()) {
                    if (!l.startsWith("#") && l.contains(".ts")) {
                        l = resolveRelativeUrl(l, baseUrl);
                    }
                    result.add(l);
                }
            }
        }

        return Pair.of(adLineCount, result);
    }

    /**
     * 判断是否为广告片段（使用动态阈值）
     */
    private boolean isAdSegment(Segment segment, int threshold) {
        return !segment.isFirstSegment() && segment.getTsCount() < threshold;
    }

    /**
     * 获取基础路径
     */
    private String getBaseUrl(String url) {
        int lastSlash = url.lastIndexOf(StrPool.SLASH);

        if (lastSlash != -1) {
            return url.substring(0, lastSlash + 1);
        }

        return StringUtils.EMPTY;
    }

    /**
     * 解析相对URL为绝对URL
     */
    private String resolveRelativeUrl(String relativeUrl, String baseUrl) {
        if (StringUtils.isBlank(baseUrl) || relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }

        return baseUrl + relativeUrl;
    }

    /**
     * 片段类
     * 用于正态分布暴力过滤
     */
    @Data
    private static class Segment {
        private final int startLineNo;
        private final List<String> lines;
        private final int tsCount;
        private final boolean startsWithDiscontinuity;
        private final boolean firstSegment;

        public Segment(int startLineNo, List<String> lines, boolean firstSegment) {
            this.startLineNo = startLineNo;
            this.lines = new ArrayList<>(lines);
            this.tsCount = countTsFiles(lines);
            this.startsWithDiscontinuity = !lines.isEmpty() &&
                    lines.get(0).startsWith("#EXT-X-DISCONTINUITY");
            this.firstSegment = firstSegment;
        }

        private int countTsFiles(List<String> lines) {
            int count = 0;
            for (String line : lines) {
                if (!line.startsWith("#") && line.contains(".ts")) {
                    count++;
                }
            }
            return count;
        }
    }
}