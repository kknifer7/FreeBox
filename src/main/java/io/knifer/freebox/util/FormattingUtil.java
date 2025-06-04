package io.knifer.freebox.util;

/**
 * 格式化工具类
 * @link <a href="https://blog.csdn.net/keplerpig/article/details/78094028">来源</a>
 *
 * @author Knifer
 * @version 1.0.0
 */
public final class FormattingUtil {

    // 进制位
    final static int JZ = 1024;
    // 1Byte
    final static int B = 1;
    // 1KB
    final static long KB = B * JZ;
    // 1MB
    final static long MB = KB * JZ;
    // 1GB
    final static long GB = MB * JZ;
    // 1TB
    final static long TB = GB * JZ;
    // 1PB
    final static long PB = TB * JZ;
    // EB (最多7EB)
    final static long EB = PB * JZ;

    // ZB(long 不能存储ZB字节)
    // final static long ZB = EB * JZ;
    /**
     * 格式化显示文件大小:<br>
     * 1KB=1024B<br>
     * 1MB=1024KB<br>
     * 1GB=1024MB<br>
     * 1TB=1024GB<br>
     * 1PB=1024TB<br>
     * 1EB=1024PB<br>
     * 1ZB =1024EB<br>
     * 1YB =1024ZB<br>
     * 1BB=1024YB<br>
     *
     * @param size 文件大小（字节）
     * @param precision
     *            精度 0~6
     * @return 格式化后的文本
     */
    public static String sizeFormat(long size, int precision) {
        if (precision > 6) {
            precision = 6;
        } else if (precision < 0) {
            precision = 0;
        }
        String format = "%." + precision + "f %s";
        double val = 0.0;
        String unit = "B";
        if (size <= 0) {
            return String.format(format, val, unit);
        }
        long T;
        if (size < MB) {
            // KB范围
            T = KB;
            unit = "KB";
        } else if (size < GB) {
            // MB 范围
            T = MB;
            unit = "MB";
        } else if (size < TB) {
            // GB
            T = GB;
            unit = "GB";
        } else if (size < PB) {
            // TB
            T = TB;
            unit = "TB";
        } else if (size < EB) {
            // PB
            T = PB;
            unit = "PB";
        } else {
            T = EB;
            unit = "EB";
        }

        val = (double) size / T + (size * 1.0 % T / T);
        // size%1024=KB
        // size%(1024*1024)=MB
        // size%(1024*1024*1024)=GB
        // size%(1024*1024*1024*1024)=TB
        // size%(1024*1024*1024*1024*1024)=PB
        // size%(1024*1024*1024*1024*1024*1024)=EB
        // size%(1024*1024*1024*1024*1024*1024*1024)=ZB
        // size%(1024*1024*1024*1024*1024*1024*1024*1024)=YB
        // size%(1024*1024*1024*1024*1024*1024*1024*1024*1024)=BB

        return String.format(format, val, unit);
    }

    /**
     * 格式化显示文件大小:<br>
     * 1KB=1024B<br>
     * 1MB=1024KB<br>
     * 1GB=1024MB<br>
     * 1TB=1024GB<br>
     * 1PB=1024TB<br>
     * 1EB=1024PB<br>
     * 1ZB =1024EB<br>
     * 1YB =1024ZB<br>
     * 1BB=1024YB<br>
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文本
     */
    public static String sizeFormat(Long size) {
        return sizeFormat(size, 2);
    }

    private FormattingUtil(){
        throw new AssertionError();
    }
}
