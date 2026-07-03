package ca.corbett.musicplayer.extensions.ai;

import ca.corbett.extras.properties.AbstractProperty;
import ca.corbett.extras.properties.DecimalProperty;
import ca.corbett.extras.properties.EnumProperty;
import ca.corbett.extras.properties.IntegerProperty;
import ca.corbett.extras.properties.LabelProperty;
import ca.corbett.musicplayer.AppConfig;
import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FloatingHexagonsVisualizer displays gently drifting hexagons across a soft, shifting gradient
 * background. Each hexagon has a translucent gradient fill, a subtle glow, and a faint wireframe
 * outline. They drift with a gentle sine-wave motion simulating a soft breeze, with slight rotation
 * and layered depth for visual richness.
 */
public class FloatingHexagons_Qwen36_35B_A3B extends VisualizationManager.Visualizer {

    public static final String NAME = "Floating hexagons (Qwen3.6-36B-A3B)";

    private static final String BREEZE_STRENGTH_PROP = "Visualizers.Floating Hexagons (Qwen 36B).breezeStrength";
    private static final String HEX_DENSITY_PROP = "Visualizers.Floating Hexagons (Qwen 36B).hexDensity";
    private static final String SIZE_VARIATION_PROP = "Visualizers.Floating Hexagons (Qwen 36B).sizeVariation";
    private static final String GLOW_INTENSITY_PROP = "Visualizers.Floating Hexagons (Qwen 36B).glowIntensity";
    private static final String ROTATION_SPEED_PROP = "Visualizers.Floating Hexagons (Qwen 36B).rotationSpeed";
    private static final String COLOR_PALETTE_PROP = "Visualizers.Floating Hexagons (Qwen 36B).colorPalette";

    private static final String BACKGROUND_STYLE_PROP = "Visualizers.Floating Hexagons (Qwen 36B).backgroundStyle";

    private int width;
    private int height;

    private float breezeStrength;
    private int hexCount;
    private float sizeFactor;
    private float glowIntensity;
    private float rotationSpeed;
    private ColorPalette palette;
    private BackgroundStyle bgStyle;

    private final java.util.List<Hexagon> hexagons = new ArrayList<>();
    private final Random rand = new Random();
    private BufferedImage bgBuffer;
    private float bgPhase;

    public enum ColorPalette {
        PASTEL_SKY("Pastel sky", new int[][]{
            {173, 216, 255}, // light sky blue
            {200, 180, 240}, // soft lavender
            {176, 224, 200}, // mint
            {255, 200, 220}, // soft pink
            {200, 230, 255}, // periwinkle
        }),
        AURORA("Aurora", new int[][]{
            {100, 255, 200}, // mint green
            {150, 200, 255}, // soft blue
            {200, 150, 255}, // lavender
            {100, 200, 255}, // cyan-blue
            {255, 180, 220}, // pink
        }),
        SUNSET("Sunset", new int[][]{
            {255, 180, 130}, // peach
            {255, 200, 160}, // light apricot
            {255, 160, 180}, // soft rose
            {255, 220, 180}, // pale apricot
            {240, 180, 220}, // mauve
        }),
        OCEAN("Ocean breeze", new int[][]{
            {120, 200, 240}, // ocean blue
            {150, 220, 230}, // aqua
            {180, 210, 240}, // sky blue
            {100, 180, 220}, // deep sky
            {200, 230, 240}, // seafoam
        }),
        GARDEN("Garden", new int[][]{
            {200, 240, 200}, // light green
            {255, 220, 230}, // pale pink
            {255, 240, 180}, // pale yellow
            {220, 200, 255}, // light purple
            {255, 200, 200}, // pale red
        });

        private final String label;
        private final int[][] colors;

        ColorPalette(String label, int[][] colors) {
            this.label = label;
            this.colors = colors;
        }

        @Override
        public String toString() {
            return label;
        }

        public int[][] getColors() {
            return colors;
        }
    }

    public enum BackgroundStyle {
        GRADIENT_SHIFT("Shifting gradient"),
        SOFT_CLOUDS("Soft clouds"),
        SUBTLE_STARS("Subtle stars");

        private final String label;

        BackgroundStyle(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class Hexagon {
        float x, y;
        float vx, vy;
        float baseSize;
        float rotation;
        float rotationSpeed;
        float phase;
        float driftPhase;
        float alpha;
        int colorIndex;
        int[] color;
        float wobbleSpeed;
        float wobbleAmplitude;
        float depth; // 0=far, 1=near (affects size, speed, alpha)

        Hexagon(float x, float y, float size, float depth, Random r) {
            this.x = x;
            this.y = y;
            this.baseSize = size;
            this.depth = depth;
            this.rotation = r.nextFloat() * (float) Math.PI * 2;
            this.rotationSpeed = (r.nextFloat() - 0.5f) * 0.005f;
            this.phase = r.nextFloat() * (float) Math.PI * 2;
            this.driftPhase = r.nextFloat() * (float) Math.PI * 2;
            this.wobbleSpeed = 0.3f + r.nextFloat() * 0.7f;
            this.wobbleAmplitude = 0.3f + r.nextFloat() * 0.7f;
            this.colorIndex = r.nextInt(5);
        }
    }

    public FloatingHexagons_Qwen36_35B_A3B() {
        super(NAME);
    }

    public List<AbstractProperty> getProperties() {
        List<AbstractProperty> props = new ArrayList<>();
        props.add(LabelProperty.createLabel("Visualizers.Floating Hexagons.label",
                                            "<html>The " + NAME + " visualizer shows gently drifting hexagons<br>" +
                                                "across a soft, shifting background. Each hexagon has a translucent<br>" +
                                                "gradient fill with a subtle glow and faint wireframe outline.</html>"
        ));
        props.add(new DecimalProperty(BREEZE_STRENGTH_PROP, "Breeze strength:", 1.0, 0.1, 5.0, 0.1));
        props.add(new IntegerProperty(HEX_DENSITY_PROP, "Hexagon count:", 25, 5, 80, 1));
        props.add(new DecimalProperty(SIZE_VARIATION_PROP, "Size range:", 1.0, 0.3, 3.0, 0.1));
        props.add(new DecimalProperty(GLOW_INTENSITY_PROP, "Glow intensity:", 0.5, 0.0, 1.0, 0.1));
        props.add(new DecimalProperty(ROTATION_SPEED_PROP, "Rotation speed:", 1.0, 0.0, 3.0, 0.1));
        props.add(new EnumProperty<ColorPalette>(COLOR_PALETTE_PROP, "Color palette:", ColorPalette.PASTEL_SKY));
        props.add(new EnumProperty<BackgroundStyle>(BACKGROUND_STYLE_PROP, "Background style:", BackgroundStyle.GRADIENT_SHIFT));
        return props;
    }

    @Override
    public void initialize(int width, int height) {
        this.width = width;
        this.height = height;

        breezeStrength = (float) ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                             .getProperty(BREEZE_STRENGTH_PROP)).getValue();
        hexCount = ((IntegerProperty) AppConfig.getInstance().getPropertiesManager()
                                               .getProperty(HEX_DENSITY_PROP)).getValue();
        sizeFactor = (float) ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                         .getProperty(SIZE_VARIATION_PROP)).getValue();
        glowIntensity = (float) ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                            .getProperty(GLOW_INTENSITY_PROP)).getValue();
        rotationSpeed = (float) ((DecimalProperty) AppConfig.getInstance().getPropertiesManager()
                                                            .getProperty(ROTATION_SPEED_PROP)).getValue();
        palette = ((EnumProperty<ColorPalette>) AppConfig.getInstance().getPropertiesManager()
                                                         .getProperty(COLOR_PALETTE_PROP)).getSelectedItem();
        bgStyle = ((EnumProperty<BackgroundStyle>) AppConfig.getInstance().getPropertiesManager()
                                                            .getProperty(BACKGROUND_STYLE_PROP)).getSelectedItem();

        bgPhase = 0;

        // Initialize hexagons
        hexagons.clear();
        float minDim = Math.min(width, height);

        for (int i = 0; i < hexCount; i++) {
            float depth = 0.2f + (rand.nextFloat() * 0.8f);
            float size = (minDim / (hexCount > 40 ? 12 : 18)) * (0.4f + depth * 0.6f) * (0.5f + rand.nextFloat() * (sizeFactor - 0.5f));
            Hexagon hex = new Hexagon(
                rand.nextFloat() * width,
                rand.nextFloat() * height,
                size,
                depth,
                rand
            );
            hex.vx = (0.2f + depth * 0.5f) * breezeStrength * (0.5f + rand.nextFloat() * 0.5f);
            hex.vy = (0.1f + depth * 0.3f) * breezeStrength * (rand.nextFloat() * 0.4f - 0.2f);
            hex.color = palette.getColors()[hex.colorIndex];
            hexagons.add(hex);
        }

        // Create background buffer
        bgBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {
        // Render background
        renderBackground(g);

        // Set rendering hints for smooth rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw hexagons
        float time = System.currentTimeMillis() * 0.001f;

        for (Hexagon hex : hexagons) {
            // Update position with gentle sine-wave drift
            float driftX = (float) Math.sin(time * hex.wobbleSpeed + hex.driftPhase) * hex.wobbleAmplitude * breezeStrength;
            float driftY = (float) Math.cos(time * hex.wobbleSpeed * 0.7f + hex.driftPhase + 1.0f) * hex.wobbleAmplitude * breezeStrength * 0.5f;

            hex.x += hex.vx + driftX * 0.3f;
            hex.y += hex.vy + driftY * 0.3f;

            // Wrap around edges with margin
            float margin = hex.baseSize * 2;
            if (hex.x > width + margin) hex.x = -margin;
            if (hex.x < -margin) hex.x = width + margin;
            if (hex.y > height + margin) hex.y = -margin;
            if (hex.y < -margin) hex.y = height + margin;

            // Update rotation
            hex.rotation += hex.rotationSpeed * rotationSpeed;

            // Draw hexagon
            drawHexagon(g, hex, time);
        }
    }

    private void renderBackground(Graphics2D g) {
        bgPhase += 0.002f * breezeStrength;

        switch (bgStyle) {
            case GRADIENT_SHIFT:
                renderGradientBackground(g);
                break;
            case SOFT_CLOUDS:
                renderCloudBackground(g);
                break;
            case SUBTLE_STARS:
                renderStarsBackground(g);
                break;
        }

        // Copy background to main graphics
        g.drawImage(bgBuffer, 0, 0, null);
    }

    private void renderGradientBackground(Graphics2D g) {
        Graphics2D bg = (Graphics2D) bgBuffer.getGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float phase1 = bgPhase;
        float phase2 = bgPhase * 1.3f + 1.0f;

        // Create a soft, shifting gradient
        int w = width;
        int h = height;

        // Blend between palette colors based on time
        int[][] colors = palette.getColors();
        float t1 = (float) Math.sin(phase1) * 0.5f + 0.5f;
        float t2 = (float) Math.sin(phase2) * 0.5f + 0.5f;

        Color c1 = interpolateColor(colors[0], colors[1], t1);
        Color c2 = interpolateColor(colors[2], colors[3], t2);
        Color c3 = interpolateColor(colors[4], colors[0], (t1 + t2) * 0.5f);

        // Create a multi-stop gradient
        java.awt.GradientPaint gp1 = new java.awt.GradientPaint(
            0f, 0f, c1, w, h, c2, false);
        java.awt.GradientPaint gp2 = new java.awt.GradientPaint(
            w, 0f, c3, 0, h, c1, false);

        bg.setColor(new Color(240, 248, 255));
        bg.fillRect(0, 0, w, h);
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        bg.setPaint(gp1);
        bg.fillRect(0, 0, w, h);
        bg.setPaint(gp2);
        bg.fillRect(0, 0, w, h);
        bg.dispose();
    }

    private void renderCloudBackground(Graphics2D g) {
        Graphics2D bg = (Graphics2D) bgBuffer.getGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = width;
        int h = height;

        // Base light color
        bg.setColor(new Color(245, 248, 252));
        bg.fillRect(0, 0, w, h);

        // Soft cloud-like shapes
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        int[][] colors = palette.getColors();

        for (int i = 0; i < 6; i++) {
            float cx = ((float) Math.sin(bgPhase * 0.3f + i * 1.7f) * 0.5f + 0.5f) * w;
            float cy = ((float) Math.cos(bgPhase * 0.2f + i * 2.3f) * 0.5f + 0.5f) * h;
            float radiusX = 150 + i * 40;
            float radiusY = 80 + i * 25;

            Color c = new Color(colors[i % 5][0], colors[i % 5][1], colors[i % 5][2]);
            java.awt.RadialGradientPaint rgp = new java.awt.RadialGradientPaint(
                cx, cy, radiusX,
                new float[]{0f, 0.6f, 1.0f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), 80),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 30),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)});

            bg.setPaint(rgp);
            bg.fillOval((int) (cx - radiusX), (int) (cy - radiusY),
                        (int) (radiusX * 2), (int) (radiusY * 2));
        }
        bg.dispose();
    }

    private void renderStarsBackground(Graphics2D g) {
        Graphics2D bg = (Graphics2D) bgBuffer.getGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = width;
        int h = height;

        // Dark base
        bg.setColor(new Color(20, 25, 40));
        bg.fillRect(0, 0, w, h);

        // Subtle gradient overlay
        float t = (float) Math.sin(bgPhase) * 0.5f + 0.5f;
        Color overlay1 = interpolateColor(new Color(30, 40, 80), new Color(40, 20, 60), t);
        Color overlay2 = interpolateColor(new Color(20, 50, 70), new Color(50, 30, 50), t);

        java.awt.GradientPaint gp = new java.awt.GradientPaint(
            0f, 0f, overlay1, w, h, overlay2, false);
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        bg.setPaint(gp);
        bg.fillRect(0, 0, w, h);

        // Twinkling stars
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        int[][] colors = palette.getColors();
        for (int i = 0; i < 50; i++) {
            float sx = ((i * 137.508f) % w);
            float sy = ((i * 97.31f) % h);
            float twinkle = (float) Math.sin(bgPhase * 2 + i) * 0.5f + 0.5f;
            float size = 1 + twinkle * 2;
            Color starColor = new Color(colors[i % 5][0], colors[i % 5][1], colors[i % 5][2],
                                        (int) (twinkle * 180));

            bg.setColor(starColor);
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f + twinkle * 0.5f));
            bg.fillOval((int) sx, (int) sy, (int) size, (int) size);
        }
        bg.dispose();
    }

    private void drawHexagon(Graphics2D g, Hexagon hex, float time) {
        int r = (int) hex.baseSize;
        if (r < 2) return;

        // Create hexagon path
        Path2D.Float hexPath = new Path2D.Float();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            float px = (float) (hex.x + r * Math.cos(angle));
            float py = (float) (hex.y + r * Math.sin(angle));
            if (i == 0) {
                hexPath.moveTo(px, py);
            } else {
                hexPath.lineTo(px, py);
            }
        }
        hexPath.closePath();

        // Calculate alpha based on depth and position
        float depthAlpha = 0.15f + hex.depth * 0.35f;
        float pulse = (float) Math.sin(time * 0.5 + hex.phase) * 0.05f;
        float alpha = Math.max(0.05f, Math.min(0.6f, depthAlpha + pulse));

        // Apply glow effect
        if (glowIntensity > 0.1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * glowIntensity * 0.3f));
            g.setColor(new Color(hex.color[0], hex.color[1], hex.color[2]));
            g.setStroke(new java.awt.BasicStroke(r * 0.15f));
            g.draw(hexPath);
        }

        // Gradient fill
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Create gradient fill
        float gradX = hex.x - r;
        float gradY = hex.y - r;
        float gradW = r * 2;
        float gradH = r * 2;

        int[] c = hex.color;
        Color fillColor = new Color(c[0], c[1], c[2]);
        Color lightFill = new Color(Math.min(255, c[0] + 60), Math.min(255, c[1] + 60), Math.min(255, c[2] + 60));
        Color darkFill = new Color(Math.max(0, c[0] - 30), Math.max(0, c[1] - 30), Math.max(0, c[2] - 30));

        java.awt.RadialGradientPaint fillPaint = new java.awt.RadialGradientPaint(
            hex.x, hex.y, r * 0.8f,
            new float[]{0f, 0.5f, 1.0f},
            new Color[]{lightFill, fillColor, darkFill},
            java.awt.RadialGradientPaint.CycleMethod.NO_CYCLE);

        g.setPaint(fillPaint);
        g.fill(hexPath);

        // Wireframe outline
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.5f));
        g.setColor(new Color(c[0], c[1], c[2], 120));
        g.setStroke(new java.awt.BasicStroke(1f));
        g.draw(hexPath);

        // Inner highlight for depth
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.2f));
        Path2D.Float innerPath = new Path2D.Float();
        float innerScale = 0.5f;
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            float px = (float) (hex.x + r * innerScale * Math.cos(angle));
            float py = (float) (hex.y + r * innerScale * Math.sin(angle));
            if (i == 0) {
                innerPath.moveTo(px, py);
            } else {
                innerPath.lineTo(px, py);
            }
        }
        innerPath.closePath();

        Color highlightColor = new Color(255, 255, 255, 40);
        g.setColor(highlightColor);
        g.fill(innerPath);
    }

    private Color interpolateColor(int[] c1, int[] c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (c1[0] * (1 - t) + c2[0] * t);
        int g = (int) (c1[1] * (1 - t) + c2[1] * t);
        int b = (int) (c1[2] * (1 - t) + c2[2] * t);
        return new Color(Math.min(255, Math.max(0, r)),
                         Math.min(255, Math.max(0, g)),
                         Math.min(255, Math.max(0, b)));
    }

    private Color interpolateColor(Color c1, Color c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (c1.getRed() * (1 - t) + c2.getRed() * t);
        int g = (int) (c1.getGreen() * (1 - t) + c2.getGreen() * t);
        int b = (int) (c1.getBlue() * (1 - t) + c2.getBlue() * t);
        return new Color(Math.min(255, Math.max(0, r)),
                         Math.min(255, Math.max(0, g)),
                         Math.min(255, Math.max(0, b)));
    }

    @Override
    public void stop() {
        hexagons.clear();
        if (bgBuffer != null) {
            bgBuffer.flush();
            bgBuffer = null;
        }
    }
}
