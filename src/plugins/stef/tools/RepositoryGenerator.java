/**
 * 
 */
package plugins.stef.tools;

import icy.gui.frame.IcyFrame;
import icy.plugin.abstract_.PluginActionable;
import icy.system.thread.ThreadUtil;

/**
 * @author Stephane
 */
public class RepositoryGenerator extends PluginActionable
{
    @Override
    public void run()
    {
        ThreadUtil.invokeNow(new Runnable()
        {
            @Override
            public void run()
            {
                final IcyFrame frame = new IcyFrame("Repository generator", true, true);

                frame.add(new MainPanel());

                frame.setSize(760, 760);
                frame.addToMainDesktopPane();
                frame.center();
                frame.setVisible(true);
            }
        });
    }
}
