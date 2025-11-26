package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.handler.MovieRankFetchingHandler;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.util.AsyncUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.util.List;
import java.util.function.Consumer;

/**
 * TV界面 - 影视热搜弹出框
 *
 * @author Knifer
 */
public class MovieRankPopOver extends PopOver {

    private List<String> movieRankList = List.of();

    public MovieRankPopOver(MovieRankFetchingHandler handler, Consumer<String> onAction) {
        super();

        VBox root = new VBox(3.0);
        List<Node> children = root.getChildren();

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(5, 10, 10, 10));
        setOnShowing(ignored -> {
            if (!movieRankList.isEmpty()) {

                return;
            }
            AsyncUtil.execute(() -> {
                movieRankList = handler.handle();
                if (movieRankList.isEmpty()) {

                    return;
                }
                Platform.runLater(() -> {
                    children.clear();
                    movieRankList.forEach(movieRank -> {
                        Label label = new Label(movieRank);

                        label.getStyleClass().add("link-label");
                        label.setOnMouseClicked(evt -> {
                            if (evt.getButton() != MouseButton.PRIMARY) {

                                return;
                            }
                            hide();
                            onAction.accept(movieRank);
                        });
                        children.add(label);
                    });
                });
            });
        });
        setTitle(I18nHelper.get(I18nKeys.TV_MOVIE_RANK));
        setCloseButtonEnabled(false);
        setDetachable(false);
        setArrowLocation(ArrowLocation.RIGHT_TOP);
        setHeaderAlwaysVisible(true);
        setContentNode(root);
    }
}
