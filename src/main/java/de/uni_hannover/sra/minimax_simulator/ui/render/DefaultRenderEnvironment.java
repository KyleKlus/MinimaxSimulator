package de.uni_hannover.sra.minimax_simulator.ui.render;

import static com.google.common.base.Preconditions.*;

import com.sun.javafx.tk.FontMetrics;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Default {@link RenderEnvironment}.
 *
 * @author Martin L&uuml;ck
 */
public class DefaultRenderEnvironment implements RenderEnvironment {

	private final Font font;
	private final FontMetrics fontMetrics;

	public DefaultRenderEnvironment(Font font, FontMetrics fontMetrics) {
		this.font = checkNotNull(font);
		this.fontMetrics = checkNotNull(fontMetrics);
	}

	@Override
	public Color getBackgroundColor() {
		return Color.WHITE;
	}

	@Override
	public Color getForegroundColor() {
		return Color.BLACK;
	}

	@Override
	public Font getFont() {
		return this.font;
	}

	@Override
	public FontMetrics getFontMetrics() {
		return this.fontMetrics;
	}
}