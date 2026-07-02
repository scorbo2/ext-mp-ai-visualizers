package ca.corbett.musicplayer.extensions.ai;

import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.ColorProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Draws semi-transparent hexagons that drift gently across the screen at varying sizes
 * with an animated, visually impressive dynamic background featuring pulsing gradients
 * and glowing effects.
 * This was written by GitHub Copilot / Claude Sonnet 4.6.
 */
public class FloatingHexagons_Copilot extends VisualizationManager.Visualizer {

    public static final String NAME = "Floating hexagons (copilot)";
    private static final String colorPropName = "Visualizers.Floating Hexagons.hexagonColor";
    private static final String countPropName = "Visualizers.Floating Hexagons.count";
    private static final String minSizePropName = "Visualizers.Floating Hexagons.minSize";
    private static final String maxSizePropName = "Visualizers.Floating Hexagons.maxSize";
    private static final String speedPropName = "Visualizers.Floating Hexagons.speed";
    private static final String opacityPropName = "Visualizers.Floating Hexagons.opacity";

    private static final Random RANDOM = new Random();

    private Color hexagonColor;
    private int count;
    private int minSize;
    private int maxSize;
    private float speed;
    private float opacity;
    private final List<FloatingHexagon> hexagons = new ArrayList<>();
    private int width;
    private int height;
    private float backgroundPhase = 0f;
    private final List<BackgroundGlow> backgroundGlows = new ArrayList<>();

    public FloatingHexagons_Copilot() {
        super(NAME);
    }

    public List<AbstractProperty> getProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(LabelProperty.createLabel("Visualizers.Floating Hexagons.label",
                                            "<html>The " + NAME + " visualizer shows translucent hexagons<br>" +
                                                "floating slowly across the screen at different sizes.</html>"));
        props.add(new ColorProperty(colorPropName, "Hexagon color:", ColorSelectionType.SOLID)
                      .setSolidColor(new Color(80, 180, 255)));
        props.add(new IntegerProperty(countPropName, "Hexagon count:", 18, 4, 200, 1));
        props.add(new IntegerProperty(minSizePropName, "Minimum size:", 28, 8, 240, 1));
        props.add(new IntegerProperty(maxSizePropName, "Maximum size:", 96, 12, 360, 1));
        props.add(new DecimalProperty(speedPropName, "Float speed:", 1.0, 0.1, 4.0, 0.1));
        props.add(new DecimalProperty(opacityPropName, "Opacity:", 0.28, 0.05, 0.75, 0.01));
        return props;
    }

    @Override
    public void initialize(int width, int height) {
        this.width = width;
        this.height = height;

        hexagonColor = ((ColorProperty)AppConfig.getInstance().getPropertiesManager()
                                                .getProperty(colorPropName)).getSolidColor();
        count = ((IntegerProperty)AppConfig.getInstance().getPropertiesManager().getProperty(countPropName))
            .getValue();
        minSize = ((IntegerProperty)AppConfig.getInstance().getPropertiesManager().getProperty(minSizePropName))
            .getValue();
        maxSize = ((IntegerProperty)AppConfig.getInstance().getPropertiesManager().getProperty(maxSizePropName))
            .getValue();
        speed = (float)((DecimalProperty)AppConfig.getInstance().getPropertiesManager().getProperty(speedPropName))
            .getValue();
        opacity = (float)((DecimalProperty)AppConfig.getInstance().getPropertiesManager()
                                                    .getProperty(opacityPropName))
            .getValue();

        count = clamp(count, 1, 200);
        if (minSize > maxSize) {
            int temp = minSize;
            minSize = maxSize;
            maxSize = temp;
        }
        minSize = Math.max(4, minSize);
        maxSize = Math.max(minSize, maxSize);
        hexagons.clear();
        backgroundGlows.clear();

        for (int i = 0; i < count; i++) {
            hexagons.add(createHexagon());
        }

        for (int i = 0; i < 3; i++) {
            backgroundGlows.add(new BackgroundGlow(
                RANDOM.nextFloat() * width,
                RANDOM.nextFloat() * height,
                0.003f + RANDOM.nextFloat() * 0.007f
            ));
        }
    }

    @Override
    public void renderFrame(Graphics2D graphics, VisualizationTrackInfo trackInfo) {
        drawAnimatedBackground(graphics);
        graphics.setComposite(AlphaComposite.SrcOver);

        for (FloatingHexagon hexagon : hexagons) {
            graphics.setColor(new Color(
                hexagonColor.getRed(),
                hexagonColor.getGreen(),
                hexagonColor.getBlue(),
                clamp(Math.round(255.0f * opacity * hexagon.alphaScale), 0, 255)));
            Polygon polygon = createHexagonPolygon(hexagon.x, hexagon.y, hexagon.size);
            graphics.fillPolygon(polygon);

            graphics.setColor(new Color(
                255,
                255,
                255,
                clamp(Math.round(255.0f * opacity * 0.45f * hexagon.alphaScale), 0, 255)));
            graphics.drawPolygon(polygon);

            hexagon.x += hexagon.vx;
            hexagon.y += hexagon.vy;
            hexagon.phase += hexagon.phaseStep;
            hexagon.x += (float)Math.sin(hexagon.phase) * hexagon.wobble;
            hexagon.y += (float)Math.cos(hexagon.phase * 0.7f) * (hexagon.wobble * 0.55f);
            wrap(hexagon);
        }

        backgroundPhase += 0.008f;
        if (backgroundPhase > 6.28f) {
            backgroundPhase -= 6.28f;
        }

        for (BackgroundGlow glow : backgroundGlows) {
            glow.update();
        }
    }

    private void drawAnimatedBackground(Graphics2D graphics) {
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);

        drawGradientLayers(graphics);
        drawBackgroundGlows(graphics);
    }

    private void drawGradientLayers(Graphics2D graphics) {
        float pulsePrimary = (float)Math.sin(backgroundPhase) * 0.5f + 0.5f;
        float pulseSec = (float)Math.sin(backgroundPhase * 0.7f) * 0.5f + 0.5f;

        int centerX = width / 2;
        int centerY = height / 2;
        float maxDist = (float)Math.sqrt(centerX * centerX + centerY * centerY);

        Point2D.Float center = new Point2D.Float(centerX, centerY);
        float radiusBase = maxDist * (0.3f + pulsePrimary * 0.2f);

        float[] fractions = {0f, 0.4f, 1f};
        Color[] colors = {
            new Color(
                clamp((int)(50 + pulsePrimary * 80), 0, 255),
                clamp((int)(100 + pulsePrimary * 100), 0, 255),
                clamp((int)(150 + pulseSec * 80), 0, 255),
                30
            ),
            new Color(
                clamp((int)(20 + pulseSec * 40), 0, 255),
                clamp((int)(50 + pulsePrimary * 60), 0, 255),
                clamp((int)(100 + pulseSec * 100), 0, 255),
                15
            ),
            new Color(0, 0, 0, 0)
        };

        RadialGradientPaint gradientPaint = new RadialGradientPaint(center, radiusBase, fractions, colors);
        graphics.setPaint(gradientPaint);
        graphics.fillRect(0, 0, width, height);

        float pulse2 = (float)Math.sin(backgroundPhase * 1.5f) * 0.5f + 0.5f;
        float radiusSecondary = maxDist * (0.15f + pulse2 * 0.1f);

        Color[] colors2 = {
            new Color(
                clamp((int)(80 + pulse2 * 100), 0, 255),
                clamp((int)(150 + pulseSec * 50), 0, 255),
                clamp((int)(200 + pulsePrimary * 50), 0, 255),
                20
            ),
            new Color(100, 180, 255, 0)
        };

        float[] fractions2 = {0f, 1f};
        RadialGradientPaint gradientPaint2 = new RadialGradientPaint(center, radiusSecondary, fractions2, colors2);
        graphics.setPaint(gradientPaint2);
        graphics.fillRect(0, 0, width, height);
    }

    private void drawBackgroundGlows(Graphics2D graphics) {
        for (BackgroundGlow glow : backgroundGlows) {
            float intensity = (float)Math.sin(glow.phase) * 0.5f + 0.5f;
            graphics.setColor(new Color(
                clamp((int)(100 * intensity), 0, 255),
                clamp((int)(150 * intensity), 0, 255),
                clamp((int)(200 * intensity), 0, 255),
                clamp((int)(40 * intensity), 0, 255)
            ));

            int glowRadius = (int)(100 + intensity * 100);
            graphics.fillOval(
                (int)glow.x - glowRadius,
                (int)glow.y - glowRadius,
                glowRadius * 2,
                glowRadius * 2
            );
        }
    }

    @Override
    public void stop() {
        hexagons.clear();
        backgroundGlows.clear();
    }

    private FloatingHexagon createHexagon() {
        int size = minSize + RANDOM.nextInt((maxSize - minSize) + 1);
        float baseSpeed = Math.max(0.1f, speed);
        float direction = (float)(RANDOM.nextFloat() * Math.PI * 2.0);
        float travelSpeed = baseSpeed * (0.20f + RANDOM.nextFloat() * 0.45f);
        float wobble = Math.max(0.15f, size * 0.015f);

        return new FloatingHexagon(
            RANDOM.nextFloat() * width,
            RANDOM.nextFloat() * height,
            (float)Math.cos(direction) * travelSpeed,
            (float)Math.sin(direction) * travelSpeed * 0.75f,
            size,
            wobble,
            0.01f + (RANDOM.nextFloat() * 0.02f),
            0.55f + (RANDOM.nextFloat() * 0.45f));
    }

    private void wrap(FloatingHexagon hexagon) {
        int margin = hexagon.size;
        if (hexagon.x < -margin) {
            hexagon.x = width + margin;
        }
        else if (hexagon.x > width + margin) {
            hexagon.x = -margin;
        }

        if (hexagon.y < -margin) {
            hexagon.y = height + margin;
        }
        else if (hexagon.y > height + margin) {
            hexagon.y = -margin;
        }
    }

    private Polygon createHexagonPolygon(float centerX, float centerY, int radius) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60.0 * i - 30.0);
            polygon.addPoint(
                Math.round(centerX + (float)Math.cos(angle) * radius),
                Math.round(centerY + (float)Math.sin(angle) * radius));
        }
        return polygon;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class FloatingHexagon {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private final int size;
        private final float wobble;
        private final float phaseStep;
        private final float alphaScale;
        private float phase;

        private FloatingHexagon(float x, float y, float vx, float vy, int size, float wobble,
                                float phaseStep, float alphaScale) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.wobble = wobble;
            this.phaseStep = phaseStep;
            this.alphaScale = alphaScale;
            this.phase = RANDOM.nextFloat() * (float)(Math.PI * 2.0);
        }
    }

    private static final class BackgroundGlow {
        private float x;
        private float y;
        private float speed;
        private float phase;

        private BackgroundGlow(float x, float y, float speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.phase = RANDOM.nextFloat() * (float)(Math.PI * 2.0);
        }

        private void update() {
            phase += speed;
            if (phase > 6.28f) {
                phase -= 6.28f;
            }
        }
    }
}
