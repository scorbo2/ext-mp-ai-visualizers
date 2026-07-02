package ca.corbett.musicplayer.extensions.ai;

import ca.corbett.musicplayer.ui.VisualizationManager;
import ca.corbett.musicplayer.ui.VisualizationTrackInfo;

import java.awt.Graphics2D;

/**
 * Completely optional! You can implement a custom Visualizer for your extension if you
 * want to provide full-screen visualization. If you implement this class, you should
 * update the getCustomVisualizers method in the main extension class to return an
 * instance of it so that it will be registered with the application.
 * If your extension does not intend to provide a visualizer, you can delete or ignore this class.
 */
public class VisualizerTemplate extends VisualizationManager.Visualizer {

    /**
     * The name should be short, descriptive, and distinctive.
     * This is presented to the user in the list of available visualizers.
     */
    public static final String NAME = "UNIQUE_VISUALIZER_NAME_GOES_HERE";

    public VisualizerTemplate() {
        super(NAME);
    }

    /**
     * Invoked when the Visualizer is started, to supply it with the width and height
     * of the target window and to allow it to prepare to start visualizing.
     * This will get invoked once, before you start receiving renderFrame() calls, so you
     * can do some initialization here if you need to.
     * <p>
     * If your extension requires IO-heavy or CPU-heavy initialization,
     * try to do it here instead of in renderFrame(), to keep things more efficient!
     * </p>
     *
     * @param width  The pixel width of the display area.
     * @param height The pixel height of the display area.
     */
    @Override
    public void initialize(int width, int height) {
    }

    /**
     * Invoked in the animation loop to render a single frame. The given VisualizationTrackInfo object contains
     * information about the currently playing track (if any), but you generally don't need to worry about showing
     * this info to the user. There's a bouncing "track info" overlay containing this information that is rendered on
     * top of your visualization. The user can disable that overlay with a hotkey if your visualizer
     * shows track info in a different way.
     * <p>
     * Avoid heavy IO here if you can! If you need to load images or other resources, try to do it
     * in advance, in the initialize() method instead, and cache them in memory. You can clean up
     * in the stop() method.
     * </p>
     *
     * @param g         The Graphics2D object you can use to render your frame. Don't dispose it!
     * @param trackInfo Information about the currently playing track, if any. Might be null if nothing is playing.
     */
    @Override
    public void renderFrame(Graphics2D g, VisualizationTrackInfo trackInfo) {

    }

    /**
     * Invoked when your Visualizer is stopped. Do any cleanup you need to do here.
     * After this method is invoked, it is guaranteed that your visualizer will not receive
     * another renderFrame() call without first receiving another initialize() call.
     */
    @Override
    public void stop() {
    }

    /**
     * Return true if you're going to display informational stuff in the bottom third of the window, and you
     * don't want the bouncing track info text overlay to enter that part of the screen.
     * Examples: lyrics sheet visualizer, commentary visualizer.
     * Most visualizers can just return false here. In that case,
     * the bouncing track info overlay is free to bounce anywhere on the screen.
     *
     * @return True if you want to keep the bouncing text overlay box out of the bottom third of the screen.
     */
    @Override
    public boolean reserveBottomGutter() {
        return false;
    }

}
