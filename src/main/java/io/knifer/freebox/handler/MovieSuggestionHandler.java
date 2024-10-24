package io.knifer.freebox.handler;

import org.controlsfx.control.textfield.AutoCompletionBinding;

import java.util.Collection;

/**
 * 影视建议处理器
 *
 * @author Knifer
 */
public interface MovieSuggestionHandler {

    Collection<String> handle(AutoCompletionBinding.ISuggestionRequest suggestionRequest);
}
