package io.knifer.freebox.component.node;

import cn.hutool.core.util.ReflectUtil;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import io.knifer.freebox.helper.ImageHelper;
import io.knifer.freebox.util.CastUtil;
import javafx.application.Platform;
import javafx.css.*;
import javafx.css.converter.FontConverter;
import javafx.css.converter.InsetsConverter;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class EmojiableLabel extends Region {

    private boolean updatingFont = false;
    private String originalText;

    @Getter
    private final TextFlow textFlow;

    private final StyleableObjectProperty<Paint> textFill;
    private final StyleableObjectProperty<Font> font;
    private final StyleableObjectProperty<Number> emojiSize;

    private final StyleableStringProperty fontFamily;
    private final StyleableObjectProperty<Number> fontSize;
    private final StyleableObjectProperty<FontPosture> fontStyle;
    private final StyleableObjectProperty<FontWeight> fontWeight;

    private final Map<String, List<Text>> emojiToPlaceholders = new HashMap<>();
    private final Map<Text, String> placeholderIds = new HashMap<>();

    private static final AtomicLong idCounter = new AtomicLong(0);
    private static final String TWEMOJI_BASE_URL = "https://twemoji.maxcdn.com/v/latest/72x72/";
    private static final String[] LEADING_ZEROS = {
            "000", "00", "0", ""
    };

    public EmojiableLabel() {
        this(StringUtils.EMPTY);
    }

    public EmojiableLabel(String text) {
        Font defaultFont = Font.getDefault();

        this.fontFamily = new SimpleStyleableStringProperty(StyleableProperties.FONT_FAMILY, this, "fontFamily", defaultFont.getFamily()) {
            @Override
            protected void invalidated() {
                updateFontFromProperties();
            }
        };

        this.fontSize = new SimpleStyleableObjectProperty<>(StyleableProperties.FONT_SIZE, this, "fontSize", defaultFont.getSize()) {
            @Override
            protected void invalidated() {
                updateFontFromProperties();
            }
        };

        this.fontStyle = new SimpleStyleableObjectProperty<>(StyleableProperties.FONT_STYLE, this, "fontStyle", FontPosture.REGULAR) {
            @Override
            protected void invalidated() {
                updateFontFromProperties();
            }
        };

        this.fontWeight = new SimpleStyleableObjectProperty<>(StyleableProperties.FONT_WEIGHT, this, "fontWeight", FontWeight.NORMAL) {
            @Override
            protected void invalidated() {
                updateFontFromProperties();
            }
        };

        this.font = new SimpleStyleableObjectProperty<>(StyleableProperties.FONT, this, "font", defaultFont) {
            @Override
            protected void invalidated() {
                Font newFont = get();
                String family;
                double size;
                String style;
                FontWeight weight;
                FontPosture posture;

                if (newFont != null && !updatingFont) {
                    updatingFont = true;
                    try {
                        family = newFont.getFamily();
                        if (!Objects.equals(family, fontFamily.get())) {
                            fontFamily.set(family);
                        }
                        size = newFont.getSize();
                        if (Math.abs(size - fontSize.get().doubleValue()) > 0.001) {
                            fontSize.set(size);
                        }
                        // 解析字体样式
                        style = newFont.getStyle().toLowerCase();
                        weight = parseFontWeight(style);
                        posture = parseFontPosture(style);
                        if (weight != fontWeight.get()) {
                            fontWeight.set(weight);
                        }
                        if (posture != fontStyle.get()) {
                            fontStyle.set(posture);
                        }
                    } finally {
                        updatingFont = false;
                    }
                }
            }
        };

        this.textFill = new SimpleStyleableObjectProperty<>(StyleableProperties.TEXT_FILL, this, "textFill", Color.BLACK) {
            @Override
            protected void invalidated() {
                updateTextFill();
            }
        };

        this.emojiSize = new SimpleStyleableObjectProperty<>(StyleableProperties.EMOJI_SIZE, this, "emojiSize", defaultFont.getSize()) {
            @Override
            protected void invalidated() {
                renderText();
            }
        };

        this.textFlow = new TextFlow();
        getChildren().add(textFlow);
        getStyleClass().add("emojiable-label");
        setText(text);
    }

    private void updateFontFromProperties() {
        String family;
        double size;
        FontWeight weight;
        FontPosture posture;
        Font oldFont;
        String oldFamily;
        double oldSize;
        String oldFontStyle;
        FontWeight oldWeight;
        FontPosture oldPosture;
        Font newFont;
        boolean fontChanged;

        if (updatingFont) {

            return;
        }
        updatingFont = true;
        try {
            family = fontFamily.get();
            size = fontSize.get().doubleValue();
            weight = fontWeight.get();
            posture = fontStyle.get();
            oldFont = this.font.get();
            if (oldFont == null) {
                fontChanged = false;
            } else {
                oldFamily = oldFont.getFamily();
                oldSize = oldFont.getSize();
                oldFontStyle = oldFont.getStyle().toLowerCase();
                oldWeight = parseFontWeight(oldFontStyle);
                oldPosture = parseFontPosture(oldFontStyle);
                fontChanged = !Objects.equals(family, oldFamily) ||
                        Math.abs(size - oldSize) > 0.001 ||
                        !Objects.equals(weight, oldWeight) ||
                        !Objects.equals(posture, oldPosture);
            }
            if (fontChanged) {
                newFont = Font.font(family, weight, posture, size);
                font.set(newFont);
                renderText();
            }
        } finally {
            updatingFont = false;
        }
    }

    private FontWeight parseFontWeight(String fontStyle) {
        String[] namesArr;

        for (FontWeight value : FontWeight.values()) {
            namesArr = (String[]) ReflectUtil.getFieldValue(value, "names");
            for (String name : namesArr) {
                if (StringUtils.containsIgnoreCase(fontStyle, name)) {

                    return value;
                }
            }
        }

        return FontWeight.NORMAL;
    }

    private FontPosture parseFontPosture(String fontStyle) {
        return StringUtils.containsIgnoreCase(fontStyle, "italic") ?
                FontPosture.ITALIC : FontPosture.REGULAR;
    }

    private void updateTextFill() {
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                ((Text) node).setFill(textFill.get());
            }
        }
    }

    private void updateTextFont() {
        Font currentFont = font.get();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text textNode) {
                textNode.setFont(currentFont);
                textNode.setFill(textFill.get());
            }
        }
    }

    public void setText(String text) {
        this.originalText = text != null ? text : StringUtils.EMPTY;
        emojiToPlaceholders.clear();
        placeholderIds.clear();
        renderText();
    }

    public String getText() {
        return originalText;
    }

    public final Font getFont() { return font.get(); }
    public final void setFont(Font value) { font.set(value); }
    public final StyleableObjectProperty<Font> fontProperty() { return font; }

    public final String getFontFamily() { return fontFamily.get(); }
    public final void setFontFamily(String value) { fontFamily.set(value); }
    public final StyleableStringProperty fontFamilyProperty() { return fontFamily; }

    public final double getFontSize() { return fontSize.get().doubleValue(); }
    public final void setFontSize(double value) { fontSize.set(value); }
    public final StyleableObjectProperty<Number> fontSizeProperty() { return fontSize; }

    public final FontPosture getFontStyle() { return fontStyle.get(); }
    public final void setFontStyle(FontPosture value) { fontStyle.set(value); }
    public final StyleableObjectProperty<FontPosture> fontStyleProperty() { return fontStyle; }

    public final FontWeight getFontWeight() { return fontWeight.get(); }
    public final void setFontWeight(FontWeight value) { fontWeight.set(value); }
    public final StyleableObjectProperty<FontWeight> fontWeightProperty() { return fontWeight; }

    public final Paint getTextFill() { return textFill.get(); }
    public final void setTextFill(Paint value) { textFill.set(value); }
    public final StyleableObjectProperty<Paint> textFillProperty() { return textFill; }

    public final double getEmojiSize() { return emojiSize.get().doubleValue(); }
    public final void setEmojiSize(double value) { emojiSize.set(value); }
    public final StyleableObjectProperty<Number> emojiSizeProperty() { return emojiSize; }

    private void renderText() {
        textFlow.getChildren().clear();
        if (StringUtils.isBlank(originalText)) return;

        List<Node> nodes = parseTextWithEmoji(originalText);
        textFlow.getChildren().addAll(nodes);
        updateTextFont();
    }

    private List<Node> parseTextWithEmoji(String text) {
        List<Node> nodes = new ArrayList<>();
        List<EmojiPosition> emojiPositions = new ArrayList<>();
        Text textNode;

        EmojiParser.parseFromUnicode(text, unicodeCandidate -> {
            emojiPositions.add(new EmojiPosition(
                    unicodeCandidate.getEmojiStartIndex(),
                    unicodeCandidate.getEmoji().getUnicode(),
                    unicodeCandidate.getFitzpatrickEndIndex()
            ));
            return StringUtils.EMPTY;
        });

        emojiToPlaceholders.clear();
        placeholderIds.clear();

        if (emojiPositions.isEmpty()) {
            textNode = new Text(text);
            textNode.setFont(getFont());
            textNode.setFill(getTextFill());
            nodes.add(textNode);

            return nodes;
        }

        int lastIndex = 0;
        String beforeText;
        for (EmojiPosition pos : emojiPositions) {
            if (pos.start > lastIndex) {
                beforeText = text.substring(lastIndex, pos.start);
                if (!beforeText.isEmpty()) {
                    textNode = new Text(beforeText);
                    textNode.setFont(getFont());
                    textNode.setFill(getTextFill());
                    nodes.add(textNode);
                }
            }

            Text placeholder = createPlaceholderNode(pos.emoji);
            nodes.add(placeholder);

            emojiToPlaceholders.computeIfAbsent(pos.emoji, k -> new ArrayList<>()).add(placeholder);
            placeholderIds.put(placeholder, "ph-" + idCounter.getAndIncrement());
            lastIndex = pos.end;
        }

        String remainingText;
        if (lastIndex < text.length()) {
            remainingText = text.substring(lastIndex);
            if (!remainingText.isEmpty()) {
                textNode = new Text(remainingText);
                textNode.setFont(getFont());
                textNode.setFill(getTextFill());
                nodes.add(textNode);
            }
        }

        for (Map.Entry<String, List<Text>> entry : emojiToPlaceholders.entrySet()) {
            loadEmojiForPlaceholders(entry.getKey(), entry.getValue());
        }

        return nodes;
    }

    private Text createPlaceholderNode(String emoji) {
        Text placeholder = new Text(emoji);
        placeholder.setFont(getFont());
        placeholder.setFill(getTextFill());
        placeholder.setStyle("-fx-font-size: " + getEmojiSize() + "px; -fx-opacity: 0.6;");
        return placeholder;
    }

    private void loadEmojiForPlaceholders(String emoji, List<Text> placeholders) {
        String emojiUrl = buildEmojiImageUrl(emoji);
        if (emojiUrl == null) return;

        ImageHelper.loadAsync(emojiUrl)
                .thenApply(result -> result.isSuccess() ? result.getImage() : null)
                .thenAccept(image -> {
                    if (image != null) {
                        Platform.runLater(() -> {
                            for (Text placeholder : placeholders) {
                                safelyReplacePlaceholder(placeholder, image);
                            }
                        });
                    }
                });
    }

    private void safelyReplacePlaceholder(Text placeholder, Image image) {
        if (placeholder.getScene() == null) return;

        String originalId = placeholderIds.get(placeholder);
        if (originalId == null) return;

        List<Node> siblings = textFlow.getChildren();
        int index = siblings.indexOf(placeholder);

        if (
                index != -1 &&
                siblings.get(index) == placeholder &&
                originalId.equals(placeholderIds.get(placeholder))
        ) {
            siblings.set(index, createEmojiNode(image));
        }
    }

    @Nullable
    private String buildEmojiImageUrl(String emoji) {
        try {
            Emoji emojiObj = EmojiManager.getByUnicode(emoji);
            String hexCode;
            StringBuilder hexBuilder;
            int[] codePoints;
            String hex;
            int len;

            if (emojiObj != null) {
                hexCode = emojiObj.getHtmlHexadecimal();
                if (StringUtils.isNotBlank(hexCode)) {
                    hexCode = hexCode.replace("&#x", "").replace(";", "");

                    return TWEMOJI_BASE_URL + hexCode + ".png";
                }
            }
            hexBuilder = new StringBuilder(emoji.length() * 4);
            codePoints = emoji.codePoints().toArray();
            for (int codePoint : codePoints) {
                hex = Integer.toHexString(codePoint);
                len = hex.length();
                if (len < 4) {
                    hexBuilder.append(LEADING_ZEROS[len - 1]);
                }
                hexBuilder.append(hex);
            }

            return TWEMOJI_BASE_URL + hexBuilder + ".png";
        } catch (Exception e) {
            log.error("Failed to build emoji image URL", e);
            return null;
        }
    }

    private Node createEmojiNode(Image image) {
        double emojiSize = getEmojiSize();
        double baselineOffset = getEmojiBaselineOffset();
        ImageView imageView = createEmojiImageView(image, emojiSize);
        StackPane alignedPane = new StackPane() {
            @Override
            public double getBaselineOffset() {
                return baselineOffset;
            }
        };

        StackPane.setAlignment(imageView, Pos.CENTER);
        alignedPane.setPrefSize(emojiSize, emojiSize);
        alignedPane.getChildren().add(imageView);

        return alignedPane;
    }

    private double getEmojiBaselineOffset() {
        // 创建一个临时Text节点，使用当前字体和大小，获取基线偏移，以修改emoji图片位置让图片和文本对齐
        Text dummyText = new Text("x");
        dummyText.setFont(getFont());
        dummyText.applyCss();

        return dummyText.getBaselineOffset();
    }

    private ImageView createEmojiImageView(Image image, double emojiSize) {
        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();
        double ratio = originalWidth / originalHeight;
        double displayWidth, displayHeight;

        if (ratio >= 1) {
            displayWidth = emojiSize;
            displayHeight = emojiSize / ratio;
        } else {
            displayHeight = emojiSize;
            displayWidth = emojiSize * ratio;
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(displayWidth);
        imageView.setFitHeight(displayHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        return imageView;
    }

    private static class StyleableProperties {
        private static final CssMetaData<EmojiableLabel, String> FONT_FAMILY =
                new CssMetaData<>("-fx-font-family", new StyleConverter<String, String>() {
                    @Override
                    public String convert(ParsedValue<String, String> value, Font font) {
                        String str = value.getValue();
                        if (str != null && str.length() > 1) {
                            if ((str.startsWith("'") && str.endsWith("'")) ||
                                    (str.startsWith("\"") && str.endsWith("\""))) {
                                return str.substring(1, str.length() - 1);
                            }
                        }
                        return str;
                    }
                }) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.fontFamily.isBound();
                    }
                    @Override
                    public StyleableStringProperty getStyleableProperty(EmojiableLabel node) {
                        return node.fontFamily;
                    }
                };

        private static final CssMetaData<EmojiableLabel, Number> FONT_SIZE =
                new CssMetaData<>("-fx-font-size", FontConverter.FontSizeConverter.getInstance(), Font.getDefault().getSize()) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.fontSize.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<Number> getStyleableProperty(EmojiableLabel node) {
                        return node.fontSize;
                    }
                };

        private static final CssMetaData<EmojiableLabel, FontPosture> FONT_STYLE =
                new CssMetaData<>("-fx-font-style", FontConverter.FontStyleConverter.getInstance(), FontPosture.REGULAR) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.fontStyle.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<FontPosture> getStyleableProperty(EmojiableLabel node) {
                        return node.fontStyle;
                    }
                };

        private static final CssMetaData<EmojiableLabel, FontWeight> FONT_WEIGHT =
                new CssMetaData<>("-fx-font-weight", FontConverter.FontWeightConverter.getInstance(), FontWeight.NORMAL) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.fontWeight.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<FontWeight> getStyleableProperty(EmojiableLabel node) {
                        return node.fontWeight;
                    }
                };

        private static final CssMetaData<EmojiableLabel, Font> FONT =
                new CssMetaData<>("-fx-font", FontConverter.getInstance(), Font.getDefault()) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.font.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<Font> getStyleableProperty(EmojiableLabel node) {
                        return node.font;
                    }
                };

        private static final CssMetaData<EmojiableLabel, Paint> TEXT_FILL =
                new CssMetaData<>("-fx-text-fill", PaintConverter.getInstance(), Color.BLACK) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.textFill.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<Paint> getStyleableProperty(EmojiableLabel node) {
                        return node.textFill;
                    }
                };

        private static final CssMetaData<EmojiableLabel, Number> EMOJI_SIZE =
                new CssMetaData<>("-fx-emoji-size", SizeConverter.getInstance(), Font.getDefault().getSize()) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.emojiSize.isBound();
                    }
                    @Override
                    public StyleableObjectProperty<Number> getStyleableProperty(EmojiableLabel node) {
                        return node.emojiSize;
                    }
                };

        private static final CssMetaData<EmojiableLabel, Insets> LABEL_PADDING =
                new CssMetaData<>("-fx-label-padding", InsetsConverter.getInstance(), Insets.EMPTY) {
                    @Override
                    public boolean isSettable(EmojiableLabel node) {
                        return !node.getTextFlow().paddingProperty().isBound();
                    }
                    @Override
                    public StyleableObjectProperty<Insets> getStyleableProperty(EmojiableLabel node) {
                        return CastUtil.cast(node.getTextFlow().paddingProperty());
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());

            styleables.add(FONT);
            styleables.add(FONT_FAMILY);
            styleables.add(FONT_SIZE);
            styleables.add(FONT_STYLE);
            styleables.add(FONT_WEIGHT);
            styleables.add(TEXT_FILL);
            styleables.add(EMOJI_SIZE);
            styleables.add(LABEL_PADDING);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        textFlow.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    @Override
    protected double computePrefWidth(double height) {
        return textFlow.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return textFlow.prefHeight(width);
    }

    private record EmojiPosition(int start, String emoji, int end) {}
}