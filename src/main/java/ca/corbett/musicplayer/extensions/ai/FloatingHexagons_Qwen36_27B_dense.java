package ca.corbett.musicplayer.extensions.ai;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FloatingHexagonsVisualizer renders a swarm of semi-transparent hexagons
 * that drift gently across the screen with a breeze-like motion.
 * Each hexagon uses radial gradients and layered glow strokes for a soft,
 * ethereal appearance. The background is a slowly shifting gradient.
 */
public class FloatingHexagons_Qwen36_27B_dense extends VisualizationManager.Visualizer {

    public static final String NAME = "Floating hexagons (Qwen3.6-27B-dense)";

    private static final String COUNT_PROP = "Visualizers.Floating Hexagons (Qwen 27B).count";
    private static final String MIN_SIZE_PROP = "Visualizers.Floating Hexagons (Qwen 27B).minSize";
    private static final String MAX_SIZE_PROP = "Visualizers.Floating Hexagons (Qwen 27B).maxSize";
    private static final String DRIFT_SPEED_PROP = "Visualizers.Floating Hexagons (Qwen 27B).driftSpeed";
    private static final String BREEZE_STRENGTH_PROP = "Visualizers.Floating Hexagons (Qwen 27B).breezeStrength";
    private static final String GLOW_INTENSITY_PROP = "Visualizers.Floating Hexagons (Qwen 27B).glowIntensity";
    private static final String PALETTE_PROP = "Visualizers.Floating Hexagons (Qwen 27B).palette";

    public enum Palette {
        AURORA("Aurora"),
        TWILIGHT("Twilight"),
        OCEAN("Ocean"),
        CLOUD("Cloud");

        private final String label;

        Palette(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int width;
    private int height;
    private HexParticle[] particles;
    private Random rng;
    private long frameCount;

    // Configuration values (reloaded on initialize)
    private int count;
    private int minSize;
    private int maxSize;
    private double driftSpeed;
    private double breezeStrength;
    private double glowIntensity;
    private Palette palette;

    // Background gradient precompute
    private BufferedImage bgBuffer;
    private double bgPhase;

    public FloatingHexagons_Qwen36_27B_dense() {
        super(NAME);
    }

    public List<AbstractProperty> getProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(LabelProperty.createLabel("Visualizers.Floating Hexagons (Qwen 27B).label",
                                            "<html>The " + NAME + " visualizer renders a swarm of semi-transparent<br>" +
                                                "hexagons that drift gently across the screen with a breeze-like<br>" +
                                                "motion. Each hexagon uses radial gradients and layered glow<br>" +
                                                "strokes for a soft, ethereal appearance.</html>"
        ));
        props.add(new IntegerProperty(COUNT_PROP, "Hexagon count:", 40, 8, 120, 2));
        props.add(new IntegerProperty(MIN_SIZE_PROP, "Min size:", 15, 5, 60, 1));
        props.add(new IntegerProperty(MAX_SIZE_PROP, "Max size:", 80, 30, 200, 5));
        AbstractProperty speedProp = new DecimalProperty(DRIFT_SPEED_PROP, "Drift speed:", 1.0, 0.2, 4.0, 0.1);
        speedProp.setHelpText("How fast the hexagons drift across the screen");
        props.add(speedProp);
        AbstractProperty breezeProp = new DecimalProperty(BREEZE_STRENGTH_PROP, "Breeze strength:", 3.0, 0.5, 12.0, 0.5);
        breezeProp.setHelpText("How strongly the breeze wavers the hexagons");
        props.add(breezeProp);
        AbstractProperty glowProp = new DecimalProperty(GLOW_INTENSITY_PROP, "Glow intensity:", 0.6, 0.0, 1.0, 0.05);
        glowProp.setHelpText("How pronounced the glow effect is around each hexagon");
        props.add(glowProp);
        props.add(new EnumProperty<>(PALETTE_PROP, "Color palette:", Palette.AURORA));
        return props;
    }

    @Override
    public void initialize(int width, int height) {
        this.width = width;
        this.height = height;
        this.frameCount = 0;
        this.bgPhase = 0;
        this.rng = new Random(System.nanoTime());

        // Read configuration from the properties manager
        count = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager()
                                            .getProperty(COUNT_PROP)).getValue();
        minSize = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager()
                                              .getProperty(MIN_SIZE_PROP)).getValue();
        maxSize = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager()
                                              .getProperty(MAX_SIZE_PROP)).getValue();
        driftSpeed = ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                 .getProperty(DRIFT_SPEED_PROP)).getValue();
        breezeStrength = ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                     .getProperty(BREEZE_STRENGTH_PROP)).getValue();
        glowIntensity = ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                    .getProperty(GLOW_INTENSITY_PROP)).getValue();
        palette = ((EnumProperty<Palette>) AppConfig.getInstance().getPropertiesManager()
                                                    .getProperty(PALETTE_PROP)).getSelectedItem();

        spawnParticles();
        precomputeBackground();
    }

    private void spawnParticles() {
        particles = new HexParticle[count];
        for (int i = 0; i < count; i++) {
            particles[i] = new HexParticle();
        }
    }

    private void precomputeBackground() {
        bgBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bgG = bgBuffer.createGraphics();
        bgG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // We'll redraw the background each frame since it shifts, but we use a buffer
        // approach where we only update a narrow band each frame for performance.
        // For simplicity, we'll just fill with a solid dark color and overlay a gradient.
        bgG.dispose();
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
        frameCount++;
        bgPhase += 0.002;

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw animated background gradient
        drawBackground(g);

        // Update and render each particle
        for (HexParticle p : particles) {
            p.update(frameCount, breezeStrength, driftSpeed);
            p.draw(g, glowIntensity);
        }
    }

    private void drawBackground(Graphics2D g) {
        // Deep dark background with a slowly shifting gradient overlay
        // Base: near-black
        g.setColor(new Color(0.02f, 0.02f, 0.04f));
        g.fillRect(0, 0, width, height);

        // Draw a large, soft, slowly-moving gradient blob
        float phaseShift = (float) Math.sin(bgPhase);
        float cx = width * (0.5f + 0.3f * phaseShift);
        float cy = height * (0.5f + 0.2f * (float) Math.sin(bgPhase * 0.7));
        float radius = Math.max(width, height) * 0.8f;

        Color[] colors;
        float[] fractions;

        switch (palette) {
            case AURORA:
                colors = new Color[]{
                    new Color(0.1f, 0.3f, 0.4f, 0.3f),
                    new Color(0.05f, 0.15f, 0.25f, 0.1f),
                    new Color(0f, 0f, 0f, 0f)
                };
                break;
            case TWILIGHT:
                colors = new Color[]{
                    new Color(0.25f, 0.1f, 0.3f, 0.3f),
                    new Color(0.1f, 0.05f, 0.2f, 0.1f),
                    new Color(0f, 0f, 0f, 0f)
                };
                break;
            case OCEAN:
                colors = new Color[]{
                    new Color(0.05f, 0.2f, 0.3f, 0.3f),
                    new Color(0.02f, 0.1f, 0.2f, 0.1f),
                    new Color(0f, 0f, 0f, 0f)
                };
                break;
            case CLOUD:
                colors = new Color[]{
                    new Color(0.15f, 0.15f, 0.2f, 0.3f),
                    new Color(0.08f, 0.08f, 0.12f, 0.1f),
                    new Color(0f, 0f, 0f, 0f)
                };
                break;
            default:
                colors = new Color[]{
                    new Color(0.1f, 0.2f, 0.3f, 0.3f),
                    new Color(0.05f, 0.1f, 0.2f, 0.1f),
                    new Color(0f, 0f, 0f, 0f)
                };
        }
        fractions = new float[]{0f, 0.5f, 1f};

        try {
            RadialGradientPaint bgPaint = new RadialGradientPaint(cx, cy, radius, fractions, colors);
            g.setPaint(bgPaint);
            g.fillRect(0, 0, width, height);
        } catch (IllegalArgumentException e) {
            // Fallback if gradient is invalid
            g.setColor(colors[0]);
            g.fillRect(0, 0, width, height);
        }
    }

    @Override
    public void stop() {
        if (bgBuffer != null) {
            bgBuffer.flush();
            bgBuffer = null;
        }
        particles = null;
    }

    // ---- Inner particle class ----

    private class HexParticle {
        float x, y;
        float size;
        float rotation;
        float alpha;
        Color baseColor;
        Color edgeColor;

        // Breeze motion parameters (unique per particle for organic feel)
        float phaseX, phaseY, phaseRot;
        float freqX, freqY, freqRot;
        float driftDx, driftDy;

        HexParticle() {
            x = rng.nextFloat() * width;
            y = rng.nextFloat() * height;
            size = minSize + rng.nextFloat() * (maxSize - minSize);
            rotation = rng.nextFloat() * 360f;
            alpha = 0.12f + rng.nextFloat() * 0.28f;

            // Color from palette
            float[] hsl = paletteHsl(rng);
            baseColor = Color.getHSBColor(hsl[0], hsl[1], hsl[2]);
            edgeColor = Color.getHSBColor(hsl[0], hsl[1] * 0.5f, Math.min(1f, hsl[2] * 1.5f));

            // Unique motion parameters
            phaseX = rng.nextFloat() * 2 * (float) Math.PI;
            phaseY = rng.nextFloat() * 2 * (float) Math.PI;
            phaseRot = rng.nextFloat() * 2 * (float) Math.PI;
            freqX = 0.003f + rng.nextFloat() * 0.008f;
            freqY = 0.002f + rng.nextFloat() * 0.007f;
            freqRot = 0.001f + rng.nextFloat() * 0.004f;
            driftDx = (rng.nextFloat() - 0.5f) * 0.5f;
            driftDy = (rng.nextFloat() - 0.5f) * 0.3f;
        }

        /**
         * Returns an HSL color from the current palette.
         * Returns float[3] = {hue, saturation, brightness}
         */
        float[] paletteHsl(Random rng) {
            float hue, sat, bright;
            switch (palette) {
                case AURORA:
                    // Teals, greens, soft blues, lavenders
                    float hueChoice = rng.nextFloat();
                    if (hueChoice < 0.3f) {
                        hue = 0.45f + rng.nextFloat() * 0.1f;  // teal-green
                    } else if (hueChoice < 0.6f) {
                        hue = 0.52f + rng.nextFloat() * 0.08f; // blue
                    } else {
                        hue = 0.75f + rng.nextFloat() * 0.1f;  // lavender
                    }
                    sat = 0.3f + rng.nextFloat() * 0.4f;
                    bright = 0.5f + rng.nextFloat() * 0.35f;
                    break;
                case TWILIGHT:
                    // Purples, magentas, warm pinks
                    hue = 0.7f + rng.nextFloat() * 0.2f;
                    sat = 0.25f + rng.nextFloat() * 0.45f;
                    bright = 0.45f + rng.nextFloat() * 0.35f;
                    break;
                case OCEAN:
                    // Blues, teals, cyans
                    hue = 0.5f + rng.nextFloat() * 0.12f;
                    sat = 0.2f + rng.nextFloat() * 0.4f;
                    bright = 0.4f + rng.nextFloat() * 0.4f;
                    break;
                case CLOUD:
                    // Whites, soft grays, barely-tinted pastels
                    hue = rng.nextFloat();
                    sat = 0.02f + rng.nextFloat() * 0.1f;
                    bright = 0.6f + rng.nextFloat() * 0.3f;
                    break;
                default:
                    hue = rng.nextFloat();
                    sat = 0.3f;
                    bright = 0.6f;
            }
            return new float[]{hue, sat, bright};
        }

        void update(long frame, double breezeStr, double driftSpd) {
            // Gentle sine-wave breeze motion (layered for organic feel)
            float breezeX = (float) (Math.sin(frame * freqX + phaseX) * breezeStr
                + Math.sin(frame * freqX * 1.7f + phaseX * 2.3f) * breezeStr * 0.3f);
            float breezeY = (float) (Math.sin(frame * freqY + phaseY) * breezeStr
                + Math.sin(frame * freqY * 1.3f + phaseY * 1.7f) * breezeStr * 0.3f);

            x += driftDx * (float) driftSpd + breezeX;
            y += driftDy * (float) driftSpd + breezeY;
            rotation += (float) Math.sin(frame * freqRot + phaseRot) * 0.3f;

            // Wrap around screen edges with padding
            float pad = size * 1.5f;
            if (x < -pad) x = width + pad;
            else if (x > width + pad) x = -pad;
            if (y < -pad) y = height + pad;
            else if (y > height + pad) y = -pad;
        }

        void draw(Graphics2D g, double glowInt) {
            // Build hexagon path
            GeneralPath hex = createHexagon(x, y, size, rotation);

            // Glow layers: draw larger, more transparent versions first
            if (glowInt > 0.01) {
                int glowLayers = 3;
                for (int layer = glowLayers; layer >= 1; layer--) {
                    float glowScale = 1.0f + layer * 0.12f * (float) glowInt;
                    float glowAlpha = alpha * 0.15f * (float) glowInt / layer;

                    GeneralPath glowHex = createHexagon(x, y, size * glowScale, rotation);
                    g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(),
                                         baseColor.getBlue(), (int) (glowAlpha * 255)));
                    g.fill(glowHex);
                }
            }

            // Main fill with radial gradient
            float gradRadius = size * 0.9f;
            Color centerColor = new Color(
                baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                (int) (alpha * 255));
            Color edgeColorA = new Color(
                baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                (int) (alpha * 0.15f * 255));

            try {
                RadialGradientPaint fillPaint = new RadialGradientPaint(
                    x, y, gradRadius,
                    new float[]{0f, 1f},
                    new Color[]{centerColor, edgeColorA});
                g.setPaint(fillPaint);
                g.fill(hex);
            } catch (IllegalArgumentException e) {
                g.setColor(centerColor);
                g.fill(hex);
            }

            // Crisp edge stroke
            int strokeAlpha = (int) (alpha * 1.5f * 255);
            strokeAlpha = Math.min(255, strokeAlpha);
            g.setColor(new Color(edgeColor.getRed(), edgeColor.getGreen(),
                                 edgeColor.getBlue(), strokeAlpha));
            g.setStroke(new java.awt.BasicStroke(0.8f));
            g.draw(hex);
        }

        private GeneralPath createHexagon(float cx, float cy, float r, float rotDeg) {
            GeneralPath path = new GeneralPath();
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(rotDeg + i * 60.0);
                float px = cx + (float) (r * Math.cos(angle));
                float py = cy + (float) (r * Math.sin(angle));
                if (i == 0) path.moveTo(px, py);
                else path.lineTo(px, py);
            }
            path.closePath();
            return path;
        }
    }
}
