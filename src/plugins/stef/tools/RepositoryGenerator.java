/**
 * 
 */
package plugins.stef.tools;

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
                 new MainFrame(getPreferences("setting"));
            }
        });
    }
 }   
 
