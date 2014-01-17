/**
 * 
 */
package plugins.stef.tools;

import icy.gui.frame.IcyFrame;
import icy.preferences.XMLPreferences;

/**
 * @author Stephane
 */
public class MainFrame extends IcyFrame
{
    final XMLPreferences prefs;
    final MainPanel panel;

    public MainFrame(XMLPreferences prefs)
    {
        super("Repository generator", true, true);

        this.prefs = prefs;

        panel = new MainPanel();

        add(panel);

        setSize(760, 760);
        addToMainDesktopPane();
        center();
        setVisible(true);

        panel.loadParams(prefs);
    }

    @Override
    public void onClosed()
    {
        panel.saveParams(prefs);
    }
}
