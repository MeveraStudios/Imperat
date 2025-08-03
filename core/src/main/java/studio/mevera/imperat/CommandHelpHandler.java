package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.help.HelpProvider;

public sealed interface CommandHelpHandler<S extends Source> permits ImperatConfig {


	/**
	 * @return The template for showing help
	 */
	@Nullable
	HelpProvider<S> getHelpProvider();

	/**
	 * Set the help template to use
	 *
	 * @param template the help template
	 */
	void setHelpProvider(@Nullable HelpProvider<S> template);

}
