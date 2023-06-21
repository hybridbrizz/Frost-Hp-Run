package com.ericversteeg;

import com.ericversteeg.config.BarInfo;
import com.ericversteeg.config.BarType;
import com.ericversteeg.view.*;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.util.*;
import java.awt.*;
import java.io.InputStream;
import java.util.List;

class HpBarOverlay extends RSViewOverlay {

	private final Client client;
	private final ItemManager itemManager;
	private final HpBarPlugin plugin;
	private final HpBarConfig config;

	private Font primaryFont;
	private Font secondaryFont;
	private Font smallFont;

	List<BarType> types;

	private RSColumn hpColumn;
	private RSColumn runColumn;

	private RSBar primaryBar;
	private RSTextView primaryTextView;

	private RSBar secondaryBar;
	private RSTextView secondaryTextView;

	private RSBar tertiaryBar;
	private RSTextView tertiaryTextView;

	private RSBar quaternaryBar;
	private RSTextView quaternaryTextView;

	private RSBar runBar;
	private RSTextView runTextView;

	private String hpViewName = "hp_view";
	private String runViewName = "run_view";

	@Inject
	private HpBarOverlay(
			Client client,
			ItemManager itemManager,
			HpBarPlugin plugin,
			HpBarConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		this.client = client;
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	void setupHpBar(boolean animate)
	{
		clearHpViewInfo();

		Map<BarType, BarInfo> barInfo = plugin.barInfo();

		int width = Math.min(800, Math.max(config.width(), 40));
		int height = Math.min(150, Math.max(config.height(), 20));

		setFonts(height);

		boolean hasSecondaryText = config.hasSecondaryText();

		hpColumn = new RSColumn(0,0, width, RSView.WRAP_CONTENT);

		types = types();

		Collections.reverse(types);

		for (int i = 0; i < types.size(); i++)
		{
			BarInfo info = barInfo.get(types.get(i));

			if (i < types.size() - 1)
			{
				RSBox container = new RSBox(0, 0, RSView.MATCH_PARENT, height / 4);
				if (i == types.size() - 2 && config.isLargeSecondary())
				{
					container = new RSBox(0, 0, RSView.MATCH_PARENT, height / 2);
				}

				RSBar bar = new RSBar(RSView.MATCH_PARENT, RSView.MATCH_PARENT, info.maxValue);
				bar.setHue(info.hue);
				bar.setValue(info.value);

				RSTextView text = new RSTextView(0, 0, RSView.WRAP_CONTENT, RSView.WRAP_CONTENT, smallFont);
				if (i == types.size() - 2 && config.isLargeSecondary())
				{
					text = new RSTextView(0, 0, RSView.WRAP_CONTENT, RSView.WRAP_CONTENT, smallFont);
					text.setLayoutGravity(RSViewGroup.Gravity.START);
				}

				text.setText(String.valueOf(bar.getValue()));
				text.setMarginStart(1);
				text.setMarginTop(-1);

				container.addView(bar);

				if (hasSecondaryText)
				{
					container.addView(text);
				}

				hpColumn.addView(container);

				if (i == types.size() - 2)
				{
					secondaryBar = bar;
					secondaryTextView = text;
				}
				else if (i == types.size() - 3)
				{
					tertiaryBar = bar;
					tertiaryTextView = text;
				}
				else if (i == types.size() - 4)
				{
					quaternaryBar = bar;
					quaternaryTextView = text;
				}
			}
			else
			{
				RSBox container = new RSBox(0, 0, RSView.MATCH_PARENT, height);

				RSBar bar = new RSBar(RSView.MATCH_PARENT, height, info.maxValue);
				RSTextView text = new RSTextView(0, 0, RSView.WRAP_CONTENT,
						RSView.WRAP_CONTENT, primaryFont);

				bar.setValue(info.value);
				bar.setHue(info.hue);

				text.setText(String.format("%d", bar.getValue()));
				text.setLayoutGravity(RSViewGroup.Gravity.CENTER);

				container.addView(bar);
				if (config.hasPrimaryText())
				{
					container.addView(text);
				}

				hpColumn.addView(container);

				primaryBar = bar;
				primaryTextView = text;
			}
		}

		hpColumn.setRenderReverse(true);

		if (animate)
		{
			hpColumn.animate()
					.duration(0.1f)
					.fadeIn()
					.start();
		}

		ViewInfo hpViewInfo = new ViewInfo(client, hpColumn, config.anchorType(), config.anchorX(), config.anchorY());

		addViewInfo(hpViewName, hpViewInfo);
	}

	private void updateHpBar()
	{
		Map<BarType, BarInfo> barInfo = plugin.barInfo();

		for (int i = 0; i < types.size(); i++)
		{
			BarInfo info = barInfo.get(types.get(i));

			if (i < types.size() - 1)
			{
				if (i == types.size() - 2)
				{
					secondaryBar.setValue(info.value);
					secondaryBar.setHue(info.hue);
					secondaryTextView.setText(String.valueOf(secondaryBar.getValue()));
				}
				else if (i == types.size() - 3)
				{
					tertiaryBar.setValue(info.value);
					tertiaryBar.setHue(info.hue);
					tertiaryTextView.setText(String.valueOf(tertiaryBar.getValue()));
				}
				else if (i == types.size() - 4)
				{
					quaternaryBar.setValue(info.value);
					quaternaryBar.setHue(info.hue);
					quaternaryTextView.setText(String.valueOf(quaternaryBar.getValue()));
				}
			}
			else
			{
				primaryBar.setValue(info.value);
				primaryBar.setHue(info.hue);
				primaryTextView.setText(String.valueOf(primaryBar.getValue()));
			}
		}
	}

	void setupRunBar(boolean animate)
	{
		clearRunViewInfo();

		Map<BarType, BarInfo> barInfo = plugin.barInfo();

		int width = Math.min(800, Math.max(config.runWidth(), 40));
		int height = Math.min(150, Math.max(config.runHeight(), 20));

		setFonts(height);

		runColumn = new RSColumn(0,0, width, RSView.WRAP_CONTENT);

		types = new ArrayList<>();
		types.add(BarType.RUN_ENERGY);

		BarInfo info = barInfo.get(BarType.RUN_ENERGY);

		RSBox container = new RSBox(0, 0, RSView.MATCH_PARENT, height);

		RSBar bar = new RSBar(RSView.MATCH_PARENT, height, info.maxValue);
		RSTextView text = new RSTextView(0, 0, RSView.WRAP_CONTENT,
				RSView.WRAP_CONTENT, primaryFont);

		bar.setValue(info.value);
		bar.setHue(info.hue);

		text.setText(String.format("%d", bar.getValue()));
		text.setLayoutGravity(RSViewGroup.Gravity.CENTER);

		container.addView(bar);
		container.addView(text);

		runColumn.addView(container);

		runBar = bar;
		runTextView = text;

		if (animate)
		{
			runColumn.animate()
					.duration(0.1f)
					.fadeIn()
					.start();
		}

		ViewInfo runViewInfo = new ViewInfo(client, runColumn, config.runAnchorType(),
				config.runAnchorX(), config.runAnchorY());

		addViewInfo(runViewName, runViewInfo);
	}

	private void updateRunBar()
	{
		Map<BarType, BarInfo> barInfo = plugin.barInfo();
		BarInfo info = barInfo.get(BarType.RUN_ENERGY);

		runBar.setValue(info.value);
		runBar.setHue(info.hue);
		runTextView.setText(String.valueOf(runBar.getValue()));
	}

	private void setFonts(int height)
	{
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			InputStream inputStream = FontManager.class.getResourceAsStream("runescape_bold.ttf");
			Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream)
					.deriveFont(Font.PLAIN,  (int) Math.ceil((double) height / 2));
			ge.registerFont(font);
			primaryFont = font;
		}
		catch (Exception e)
		{
			primaryFont = FontManager.getRunescapeBoldFont();
		}

		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			InputStream inputStream = FontManager.class.getResourceAsStream("runescape_bold.ttf");
			Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream)
					.deriveFont(Font.PLAIN,  (int) Math.ceil((double) height / 3));
			ge.registerFont(font);
			secondaryFont = font;
		}
		catch (Exception e)
		{
			secondaryFont = FontManager.getRunescapeSmallFont();
		}

		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			InputStream inputStream = FontManager.class.getResourceAsStream("runescape_bold.ttf");
			Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream)
					.deriveFont(Font.PLAIN,  (int) Math.ceil((double) height / 4));
			ge.registerFont(font);
			smallFont = font;
		}
		catch (Exception e)
		{
			smallFont = FontManager.getRunescapeSmallFont();
		}
	}

	private boolean isFadeOut = false;
	private boolean isFadeOutRun = false;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.isAlwaysVisible() && config.showHpBar())
		{
			setupHpBar(false);
		}
		else if (config.showHpBar())
		{
			if (isVisible() && !containsViewInfo(hpViewName))
			{
				setupHpBar(true);
			}
			else if (isVisible())
			{
				updateHpBar();
			}
			else if (containsViewInfo(hpViewName))
			{
				if (!isFadeOut)
				{
					hpColumn.animate()
							.fadeOut()
							.duration(0.10f)
							.onComplete(() -> {
								removeViewInfo(hpViewName);
								isFadeOut = false;
							})
							.start();

					isFadeOut = true;
				}
			}
		}

		if (config.isRunAlwaysVisible() && config.showRunBar())
		{
			setupRunBar(false);
		}
		else if (config.showRunBar())
		{
			if (isRunVisible() && !containsViewInfo(runViewName))
			{
				setupRunBar(true);
			}
			else if (isRunVisible())
			{
				updateRunBar();
			}
			else if (containsViewInfo(runViewName))
			{
				if (!isFadeOutRun)
				{
					runColumn.animate()
							.fadeOut()
							.duration(0.10f)
							.onComplete(() -> {
								removeViewInfo(runViewName);
								isFadeOutRun = false;
							})
							.start();

					isFadeOutRun = true;
				}
			}
		}

		return super.render(graphics);
	}

	private List<BarType> types()
	{
		BarType primary = config.primaryBarType();
		BarType secondary = config.secondaryBarType();
		BarType tertiary = config.tertiaryBarType();
		BarType quaternary = config.quaternaryBarType();

		List<BarType> barTypes = new ArrayList<>();

		if (primary != BarType.NONE)
		{
			barTypes.add(primary);
		}

		if (!barTypes.contains(secondary)
				&& secondary != BarType.NONE)
		{
			barTypes.add(secondary);
		}

		if (!barTypes.contains(tertiary)
				&& tertiary != BarType.NONE)
		{
			barTypes.add(tertiary);
		}

		if (!barTypes.contains(quaternary)
				&& quaternary != BarType.NONE)
		{
			barTypes.add(quaternary);
		}

		return barTypes;
	}

	boolean hasBarType(BarType barType) {
		for (BarType type: types()) {
			if (barType == type) {
				return true;
			}
		}
		return false;
	}

	public boolean isVisible()
	{
		return config.isAlwaysVisible() || plugin.isActive();
	}

	public boolean isRunVisible()
	{
		return plugin.isRun() && config.showRunBar();
	}

	public void clearHpViewInfo()
	{
		removeViewInfo(hpViewName);
	}

	public void clearRunViewInfo()
	{
		removeViewInfo(runViewName);
	}
}
