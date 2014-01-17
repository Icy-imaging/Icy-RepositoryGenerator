package plugins.stef.tools;

import icy.common.Version;
import icy.file.FileUtil;
import icy.file.xml.XMLPersistent;
import icy.file.xml.XMLPersistentHelper;
import icy.gui.component.IcyTextField;
import icy.gui.component.IcyTextField.TextChangeListener;
import icy.gui.component.button.IcyButton;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.LoadDialog;
import icy.gui.dialog.SaveDialog;
import icy.plugin.PluginLoader;
import icy.plugin.PluginLoader.PluginLoaderEvent;
import icy.plugin.PluginLoader.PluginLoaderListener;
import icy.preferences.XMLPreferences;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.type.collection.CollectionUtil;
import icy.util.ClassUtil;
import icy.util.JarUtil;
import icy.util.StringUtil;
import icy.util.XMLUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MainPanel extends JPanel implements ListSelectionListener, TextChangeListener, DocumentListener,
        PluginLoaderListener
{
    static final String ID_DEFAULT_CLASSNAME = "default_classname";
    static final String ID_DEFAULT_VERSION = "default_version";
    static final String ID_DEFAULT_REQUIRED_KERNEL_VERSION = "default_required_kernel_version";
    static final String ID_DEFAULT_AUTHOR = "default_author";
    static final String ID_DEFAULT_WEB = "default_web";
    static final String ID_DEFAULT_EMAIL = "default_email";

    static final String ID_CLASSNAME = "classname";
    static final String ID_VERSION = "version";
    static final String ID_URL = "url";
    static final String ID_NAME = "name";
    static final String ID_REQUIRED_KERNEL_VERSION = "required_kernel_version";
    static final String ID_JAR_URL = "jar_url";
    static final String ID_IMAGE_URL = "image_url";
    static final String ID_ICON_URL = "icon_url";
    static final String ID_AUTHOR = "author";
    static final String ID_CHANGELOG = "changelog";
    static final String ID_WEB = "web";
    static final String ID_EMAIL = "email";
    static final String ID_DESCRIPTION = "description";
    static final String ID_DEPENDENCIES = "dependencies";
    static final String ID_DEPENDENCY = "dependency";

    public class PluginDescriptor implements XMLPersistent
    {
        protected String className;
        protected String name;
        protected PluginIdent ident;
        protected String author;
        protected String web;
        protected String email;
        protected String desc;
        protected String changesLog;

        protected final List<PluginIdent> required;

        public PluginDescriptor()
        {
            super();

            ident = new PluginIdent();
            author = "";
            web = "";
            email = "";
            desc = "";
            changesLog = "";

            required = new ArrayList<PluginIdent>();
        }

        public PluginDescriptor(icy.plugin.PluginDescriptor plugin)
        {
            this();

            ident.setClassName(plugin.getClassName());
            ident.setVersion(plugin.getVersion());
            ident.setRequiredKernelVersion(plugin.getRequiredKernelVersion());

            setClassName(plugin.getClassName());
            setVersion(plugin.getVersion());
            setRequiredKernelVersion(plugin.getRequiredKernelVersion());
            setName(plugin.getName());
            setAuthor(plugin.getAuthor());
            setWeb(plugin.getWeb());
            setEmail(plugin.getEmail());
            setDescription(plugin.getDescription());
            setChangesLog(plugin.getChangesLog());

            for (icy.plugin.PluginDescriptor.PluginIdent ident : plugin.getRequired())
                required.add(new PluginIdent(ident));
        }

        public boolean loadFromXML(String path)
        {
            return XMLPersistentHelper.loadFromXML(this, path);
        }

        public boolean loadFromXML(URL xmlUrl)
        {
            return XMLPersistentHelper.loadFromXML(this, xmlUrl);
        }

        @Override
        public boolean loadFromXML(Node node)
        {
            if (node == null)
                return false;

            // get the plugin ident
            ident.loadFromXML(node);

            setName(XMLUtil.getElementValue(node, ID_NAME, ""));
            setAuthor(XMLUtil.getElementValue(node, ID_AUTHOR, ""));
            setWeb(XMLUtil.getElementValue(node, ID_WEB, ""));
            setEmail(XMLUtil.getElementValue(node, ID_EMAIL, ""));
            setDescription(XMLUtil.getElementValue(node, ID_DESCRIPTION, ""));
            setChangesLog(XMLUtil.getElementValue(node, ID_CHANGELOG, ""));

            final Node nodeDependances = XMLUtil.getElement(node, ID_DEPENDENCIES);
            if (nodeDependances != null)
            {
                final ArrayList<Node> nodesDependances = XMLUtil.getChildren(nodeDependances, ID_DEPENDENCY);

                for (Node n : nodesDependances)
                {
                    final PluginIdent ident = new PluginIdent();
                    // required don't need URL information as we now search from classname
                    ident.loadFromXML(n);
                    if (!ident.isEmpty())
                        required.add(ident);
                }
            }

            return true;
        }

        public boolean saveToXML()
        {
            return XMLPersistentHelper.saveToXML(this, getXMLFilename());
        }

        @Override
        public boolean saveToXML(Node node)
        {
            if (node == null)
                return false;

            ident.saveToXML(node);

            XMLUtil.setElementValue(node, ID_NAME, getName());
            XMLUtil.setElementValue(node, ID_URL, getXmlUrl());
            XMLUtil.setElementValue(node, ID_JAR_URL, getJarUrl());
            XMLUtil.setElementValue(node, ID_IMAGE_URL, getImageUrl());
            XMLUtil.setElementValue(node, ID_ICON_URL, getIconUrl());
            XMLUtil.setElementValue(node, ID_AUTHOR, getAuthor());
            XMLUtil.setElementValue(node, ID_WEB, getWeb());
            XMLUtil.setElementValue(node, ID_EMAIL, getEmail());
            XMLUtil.setElementValue(node, ID_DESCRIPTION, getDescription());
            XMLUtil.setElementValue(node, ID_CHANGELOG, getChangesLog());

            final Element dependances = XMLUtil.setElement(node, ID_DEPENDENCIES);
            if (dependances != null)
            {
                XMLUtil.removeAllChildren(dependances);
                for (PluginIdent dep : required)
                    dep.saveToXML(XMLUtil.addElement(dependances, ID_DEPENDENCY));
            }

            return true;
        }

        public void setClassName(String className)
        {
            ident.className = className;
        }

        public String getClassName()
        {
            return ident.className;
        }

        public void setVersion(Version version)
        {
            ident.setVersion(version);
        }

        public void setRequiredKernelVersion(Version version)
        {
            ident.setRequiredKernelVersion(version);
        }

        public String getXmlUrl()
        {
            return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) + getXMLExtension();
        }

        public String getJarUrl()
        {
            return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) + getJarExtension();
        }

        public String getImageUrl()
        {
            return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) + getImageExtension();
        }

        public String getIconUrl()
        {
            return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) + getIconExtension();
        }

        public List<String> getDependencies()
        {
            final List<String> result = new ArrayList<String>();

            for (PluginIdent ident : required)
                result.add(ident.getClassName());

            return result;
        }

        public void setDependencies(List<String> value)
        {
            required.clear();

            for (String className : value)
            {
                final PluginIdent ident = new PluginIdent();
                ident.setClassName(className);
                required.add(ident);
            }
        }

        public void setDependenciesAsString(String text)
        {
            setDependencies(CollectionUtil.asList(text.split("\n")));
        }

        public String getDependenciesAsString()
        {
            String result = "";

            for (String s : getDependencies())
                result += s + "\n";

            return result;
        }

        /**
         * return associated filename
         */
        public String getFilename()
        {
            return getBaseLocalPath() + ClassUtil.getPathFromQualifiedName(getClassName());
        }

        public String getUrlInternal()
        {
            return ident.getUrlInternal();
        }

        public String getSimpleClassName()
        {
            return ident.getSimpleClassName();
        }

        public String getPackageName()
        {
            return ident.getPackageName();
        }

        /**
         * Returns the XML file extension.
         */
        public String getXMLExtension()
        {
            return XMLUtil.FILE_DOT_EXTENSION;
        }

        /**
         * return xml filename
         */
        public String getXMLFilename()
        {
            return getFilename() + getXMLExtension();
        }

        /**
         * return icon extension
         */
        public String getIconExtension()
        {
            return "_icon.png";
        }

        /**
         * return icon filename
         */
        public String getIconFilename()
        {
            return getFilename() + getIconExtension();
        }

        /**
         * return image extension
         */
        public String getImageExtension()
        {
            return ".png";
        }

        /**
         * return image filename
         */
        public String getImageFilename()
        {
            return getFilename() + getImageExtension();
        }

        /**
         * Returns the JAR file extension.
         */
        public String getJarExtension()
        {
            return JarUtil.FILE_DOT_EXTENSION;
        }

        /**
         * return jar filename
         */
        public String getJarFilename()
        {
            return getFilename() + getJarExtension();
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return ident.getName();
        }

        /**
         * @return the version
         */
        public Version getVersion()
        {
            return ident.getVersion();
        }

        /**
         * @return the url
         */
        public String getUrl()
        {
            // url is default XML url
            return getXmlUrl();
        }

        /**
         * @return the desc
         * @deprecated use {@link #getDescription()} instead
         */
        @Deprecated
        public String getDesc()
        {
            return getDescription();
        }

        /**
         * @return the description
         */
        public String getDescription()
        {
            return desc;
        }

        /**
         * @return the author
         */
        public String getAuthor()
        {
            return author;
        }

        /**
         * @return the web
         */
        public String getWeb()
        {
            return web;
        }

        /**
         * @return the email
         */
        public String getEmail()
        {
            return email;
        }

        /**
         * @return the changesLog
         */
        public String getChangesLog()
        {
            return changesLog;
        }

        /**
         * @return the requiredKernelVersion
         */
        public Version getRequiredKernelVersion()
        {
            return ident.getRequiredKernelVersion();
        }

        /**
         * @return the required
         */
        public List<PluginIdent> getRequired()
        {
            return new ArrayList<PluginIdent>(required);
        }

        /**
         * @param name
         *        the name to set
         */
        public void setName(String name)
        {
            ident.setName(name);
        }

        /**
         * @param author
         *        the author to set
         */
        public void setAuthor(String author)
        {
            this.author = author;
        }

        /**
         * @param web
         *        the web to set
         */
        public void setWeb(String web)
        {
            this.web = web;
        }

        /**
         * @param email
         *        the email to set
         */
        public void setEmail(String email)
        {
            this.email = email;
        }

        /**
         * @param desc
         *        the description to set
         */
        public void setDescription(String desc)
        {
            this.desc = desc;
        }

        /**
         * @param changesLog
         *        the changesLog to set
         */
        public void setChangesLog(String changesLog)
        {
            this.changesLog = changesLog;
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    public class PluginIdent implements XMLPersistent
    {
        protected String className;
        protected Version version;
        protected Version requiredKernelVersion;
        protected String name;
        protected String url;

        /**
         * 
         */
        public PluginIdent()
        {
            super();

            // default
            className = "";
            version = new Version();
            requiredKernelVersion = new Version();
            name = "";
            url = "";
        }

        public PluginIdent(icy.plugin.PluginDescriptor.PluginIdent ident)
        {
            super();

            className = ident.getClassName();
            version = ident.getVersion();
            requiredKernelVersion = ident.getRequiredKernelVersion();
            name = "";
            url = "";
        }

        @Override
        public boolean loadFromXML(Node node)
        {
            if (node == null)
                return false;

            setClassName(XMLUtil.getElementValue(node, ID_CLASSNAME, ""));
            setVersion(new Version(XMLUtil.getElementValue(node, ID_VERSION, "")));
            setRequiredKernelVersion(new Version(XMLUtil.getElementValue(node, ID_REQUIRED_KERNEL_VERSION, "")));
            setName(XMLUtil.getElementValue(node, ID_NAME, ""));
            url = XMLUtil.getElementValue(node, ID_URL, "");

            return true;
        }

        @Override
        public boolean saveToXML(Node node)
        {
            if (node == null)
                return false;

            XMLUtil.setElementValue(node, ID_CLASSNAME, getClassName());
            XMLUtil.setElementValue(node, ID_VERSION, getVersion().toString());
            XMLUtil.setElementValue(node, ID_REQUIRED_KERNEL_VERSION, getRequiredKernelVersion().toString());
            XMLUtil.setElementValue(node, ID_NAME, getName());
            XMLUtil.setElementValue(node, ID_URL, getUrl());

            return true;
        }

        public boolean isEmpty()
        {
            return StringUtil.isEmpty(className) && version.isEmpty() && requiredKernelVersion.isEmpty();
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getUrlInternal()
        {
            return url;
        }

        public String getUrl()
        {
            return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName())
                    + XMLUtil.FILE_DOT_EXTENSION;
        }

        /**
         * @return the className
         */
        public String getClassName()
        {
            return className;
        }

        /**
         * @param className
         *        the className to set
         */
        public void setClassName(String className)
        {
            this.className = className;
        }

        /**
         * return the simple className
         */
        public String getSimpleClassName()
        {
            return ClassUtil.getSimpleClassName(className);
        }

        /**
         * return the package name
         */
        public String getPackageName()
        {
            return ClassUtil.getPackageName(className);
        }

        /**
         * @param version
         *        the version to set
         */
        public void setVersion(Version version)
        {
            this.version = version;
        }

        /**
         * @return the version
         */
        public Version getVersion()
        {
            return version;
        }

        /**
         * @return the requiredKernelVersion
         */
        public Version getRequiredKernelVersion()
        {
            return requiredKernelVersion;
        }

        /**
         * @param requiredKernelVersion
         *        the requiredKernelVersion to set
         */
        public void setRequiredKernelVersion(Version requiredKernelVersion)
        {
            this.requiredKernelVersion = requiredKernelVersion;
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    // class PluginDescriptor extends icy.plugin.PluginDescriptor
    // {
    // String className;
    //
    // public PluginDescriptor()
    // {
    // super();
    // }
    //
    // public PluginDescriptor(icy.plugin.PluginDescriptor plugin)
    // {
    // super();
    //
    // ident.setClassName(plugin.getClassName());
    // ident.setVersion(plugin.getVersion());
    // ident.setRequiredKernelVersion(plugin.getRequiredKernelVersion());
    //
    // setClassName(plugin.getClassName());
    // setVersion(plugin.getVersion());
    // setRequiredKernelVersion(plugin.getRequiredKernelVersion());
    // setName(plugin.getName());
    // setAuthor(plugin.getAuthor());
    // setWeb(plugin.getWeb());
    // setEmail(plugin.getEmail());
    // setDescription(plugin.getDescription());
    // setChangesLog(plugin.getChangesLog());
    // setRepository(plugin.getRepository());
    //
    // for (PluginIdent ident : plugin.getRequired())
    // required.add(ident);
    // }
    //
    // public void setClassName(String className)
    // {
    // this.className = className;
    // }
    //
    // @Override
    // public String getClassName()
    // {
    // return className;
    // }
    //
    // public void setVersion(Version version)
    // {
    // ident.setVersion(version);
    // }
    //
    // public void setRequiredKernelVersion(Version version)
    // {
    // ident.setRequiredKernelVersion(version);
    // }
    //
    // @Override
    // public String getXmlUrl()
    // {
    // return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) +
    // getXMLExtension();
    // }
    //
    // @Override
    // public String getJarUrl()
    // {
    // return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) +
    // getJarExtension();
    // }
    //
    // @Override
    // public String getImageUrl()
    // {
    // return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) +
    // getImageExtension();
    // }
    //
    // @Override
    // public String getIconUrl()
    // {
    // return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName()) +
    // getIconExtension();
    // }
    //
    // public List<String> getDependencies()
    // {
    // final List<String> result = new ArrayList<String>();
    //
    // for (PluginIdent ident : required)
    // result.add(ident.getClassName());
    //
    // return result;
    // }
    //
    // public void setDependencies(List<String> value)
    // {
    // required.clear();
    //
    // for (String className : value)
    // {
    // final PluginIdent ident = new PluginIdent();
    // ident.setClassName(className);
    // required.add(ident);
    // }
    // }
    //
    // public void setDependenciesAsString(String text)
    // {
    // setDependencies(CollectionUtil.asList(text.split("\n")));
    // }
    //
    // public String getDependenciesAsString()
    // {
    // String result = "";
    //
    // for (String s : getDependencies())
    // result += s + "\n";
    //
    // return result;
    // }
    //
    // /**
    // * return associated filename
    // */
    // @Override
    // public String getFilename()
    // {
    // return getBaseLocalPath() + super.getFilename();
    // }
    //
    // @Override
    // public String toString()
    // {
    // return getName();
    // }
    // }
    //
    // class PluginIdent extends icy.plugin.PluginDescriptor.PluginOnlineIdent
    // {
    // public String getUrlInternal()
    // {
    // return super.getUrl();
    // }
    //
    // @Override
    // public String getUrl()
    // {
    // return getBaseOnlinePath() + ClassUtil.getPathFromQualifiedName(getClassName())
    // + XMLUtil.FILE_DOT_EXTENSION;
    // }
    //
    // @Override
    // public String toString()
    // {
    // return getName();
    // }
    // }

    class IdentListModel extends AbstractListModel
    {
        /**
         * 
         */
        private static final long serialVersionUID = -4632919608168660268L;

        private final List<PluginIdent> idents;

        public IdentListModel(List<PluginIdent> idents)
        {
            super();

            this.idents = idents;

            // sort it
            Collections.sort(idents, identComparator);
        }

        @Override
        public int getSize()
        {
            return idents.size();
        }

        @Override
        public Object getElementAt(int i)
        {
            return idents.get(i);
        }

        public void contentsChanged()
        {
            super.fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    class LocalPluginComboBoxModel extends AbstractListModel implements ComboBoxModel
    {
        /**
         * 
         */
        private static final long serialVersionUID = -907704699790762526L;

        private final List<PluginDescriptor> plugins;
        private Object selectedObject;

        public LocalPluginComboBoxModel(List<icy.plugin.PluginDescriptor> plugins)
        {
            super();

            this.plugins = new ArrayList<PluginDescriptor>();
            selectedObject = null;

            for (icy.plugin.PluginDescriptor plugin : plugins)
                this.plugins.add(new PluginDescriptor(plugin));

            // sort it
            Collections.sort(this.plugins, pluginComparator);
        }

        @Override
        public int getSize()
        {
            return plugins.size();
        }

        @Override
        public Object getElementAt(int i)
        {
            return plugins.get(i);
        }

        public void contentsChanged()
        {
            super.fireContentsChanged(this, 0, getSize() - 1);
        }

        @Override
        public void setSelectedItem(Object value)
        {
            if (selectedObject != value)
            {
                selectedObject = value;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public Object getSelectedItem()
        {
            return selectedObject;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1488142961657050688L;

    static final Comparator<PluginIdent> identComparator = new Comparator<PluginIdent>()
    {
        @Override
        public int compare(PluginIdent o1, PluginIdent o2)
        {
            return o1.toString().compareTo(o2.toString());
        }
    };
    static final Comparator<PluginDescriptor> pluginComparator = new Comparator<PluginDescriptor>()
    {
        @Override
        public int compare(PluginDescriptor o1, PluginDescriptor o2)
        {
            return o1.toString().compareTo(o2.toString());
        }
    };

    // GUI
    JList identList;
    JLabel pathLabel;
    private JTextField baseURLField;
    private JTextField defaultPluginVersionField;
    private JTextField defaultPluginMinKernelVerField;
    private JTextField defaultPluginAuthorField;
    private JTextField defaultPluginEmailField;
    private JTextField defaultPluginWebField;
    private JTextField defaultPluginClassNameField;
    private IcyTextField pluginNameField;
    private IcyTextField pluginVersionField;
    private IcyTextField pluginMinKernelVerField;
    private IcyTextField pluginClassNameField;
    private IcyTextField pluginAuthorField;
    private IcyTextField pluginEmailField;
    private IcyTextField pluginWebField;
    private JTextArea pluginDescriptionField;
    private JTextArea pluginDependenciesField;

    // internals
    final Map<PluginIdent, PluginDescriptor> pluginMap;
    PluginIdent currentIdent;
    PluginDescriptor currentPlugin;
    int pluginId;
    boolean adjustingFields;
    private JComboBox localPluginsComboBox;

    /**
     * Create the panel.
     */
    public MainPanel()
    {
        super();

        initialize();

        pluginMap = new HashMap<PluginIdent, PluginDescriptor>();
        currentIdent = null;
        currentPlugin = null;
        pluginId = 1;
        adjustingFields = false;

        updateLocalPluginsList();

        pluginNameField.addTextChangeListener(this);
        pluginVersionField.addTextChangeListener(this);
        pluginMinKernelVerField.addTextChangeListener(this);
        pluginClassNameField.addTextChangeListener(this);
        pluginAuthorField.addTextChangeListener(this);
        pluginEmailField.addTextChangeListener(this);
        pluginWebField.addTextChangeListener(this);
        pluginDescriptionField.getDocument().addDocumentListener(this);
        pluginDependenciesField.getDocument().addDocumentListener(this);

        PluginLoader.addListener(this);
    }

    private void initialize()
    {
        setLayout(new BorderLayout(0, 0));

        JPanel topPanel = new JPanel();
        topPanel.setBorder(new TitledBorder(null, "XML Repository File", TitledBorder.LEADING, TitledBorder.TOP, null,
                null));
        add(topPanel, BorderLayout.NORTH);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));

        JButton loadButton = new JButton("Load");
        loadButton.setToolTipText("Load repository informations from file");
        loadButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String defaultPath = pathLabel.getText();

                if (StringUtil.isEmpty(defaultPath))
                    defaultPath = "repository.xml";

                final String path = LoadDialog.chooseFile("Load repository file", FileUtil.getDirectory(defaultPath),
                        FileUtil.getFileName(defaultPath));

                if (!StringUtil.isEmpty(path))
                    loadXMLFile(path);
            }
        });

        final JButton btnNewButton = new JButton("New");
        btnNewButton.setToolTipText("Clear everything to create a new repository");
        btnNewButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (ConfirmDialog
                        .confirm("All unsaved data will be lost !\nAre you sure you want to create a new repository ?"))
                {
                    pluginMap.clear();
                    updatePluginList();
                }
            }
        });
        topPanel.add(btnNewButton);

        final Component horizontalStrut_2 = Box.createHorizontalStrut(20);
        topPanel.add(horizontalStrut_2);
        topPanel.add(loadButton);

        Component horizontalStrut = Box.createHorizontalStrut(8);
        topPanel.add(horizontalStrut);

        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Save repository informations to file");
        saveButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String defaultPath = pathLabel.getText();

                if (StringUtil.isEmpty(defaultPath))
                    defaultPath = "repository.xml";

                final String path = SaveDialog.chooseFile("Save repository file", FileUtil.getDirectory(defaultPath),
                        FileUtil.getFileName(defaultPath));

                if (!StringUtil.isEmpty(path))
                {
                    // final File pluginsFolder = new File(FileUtil.getDirectory(path), "plugins");
                    //
                    // if (FileUtil.exists(pluginsFolder.getAbsolutePath()))
                    // {
                    // if (!ConfirmDialog
                    // .confirm("Content of the 'plugins' folder will be replaced !\nAre you sure you want to continue ?"))
                    // return;
                    //
                    // // remove plugins folder
                    // FileUtil.delete(pluginsFolder, true);
                    // }

                    saveXMLFile(path);
                }
            }
        });
        topPanel.add(saveButton);

        Component horizontalStrut_1 = Box.createHorizontalStrut(8);
        topPanel.add(horizontalStrut_1);

        final JLabel lblRepositoryPath = new JLabel("Repository path: ");
        topPanel.add(lblRepositoryPath);

        pathLabel = new JLabel("");
        lblRepositoryPath.setLabelFor(pathLabel);
        topPanel.add(pathLabel);

        JPanel repositoryPanel = new JPanel();
        repositoryPanel.setBorder(new TitledBorder(null, "Repository Details", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        add(repositoryPanel, BorderLayout.SOUTH);
        repositoryPanel.setLayout(new BoxLayout(repositoryPanel, BoxLayout.LINE_AXIS));

        JLabel lblBaseUrl = new JLabel("Base URL  ");
        repositoryPanel.add(lblBaseUrl);

        baseURLField = new JTextField();
        baseURLField
                .setToolTipText("Base URL where the repository will be stored (ex: http://www.site.com/repository/)");
        repositoryPanel.add(baseURLField);
        baseURLField.setColumns(10);

        JPanel listPanel = new JPanel();
        listPanel.setPreferredSize(new Dimension(260, 10));
        listPanel.setMinimumSize(new Dimension(260, 10));
        listPanel.setLayout(new BorderLayout(0, 0));

        JScrollPane scrollPane = new JScrollPane();
        listPanel.add(scrollPane);
        scrollPane.setPreferredSize(new Dimension(200, 2));

        identList = new JList();
        identList.getSelectionModel().addListSelectionListener(this);
        scrollPane.setViewportView(identList);

        JLabel lblPluginList = new JLabel("Plugin list");
        lblPluginList.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblPluginList.setHorizontalAlignment(SwingConstants.CENTER);
        scrollPane.setColumnHeaderView(lblPluginList);

        JPanel listActionPanel = new JPanel();
        listPanel.add(listActionPanel, BorderLayout.SOUTH);
        GridBagLayout gbl_listActionPanel = new GridBagLayout();
        gbl_listActionPanel.columnWidths = new int[] {0, 0, 0, 0};
        gbl_listActionPanel.rowHeights = new int[] {23, 0, 0};
        gbl_listActionPanel.columnWeights = new double[] {1.0, 1.0, 0.0, Double.MIN_VALUE};
        gbl_listActionPanel.rowWeights = new double[] {0.0, 0.0, Double.MIN_VALUE};
        listActionPanel.setLayout(gbl_listActionPanel);

        JButton addButton = new JButton("New Plugin");
        addButton.setToolTipText("Add a new empty plugin in the repository");
        addButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PluginDescriptor plugin = createPlugin();
                pluginMap.put(createIdent(plugin), plugin);
                updatePluginList();
            }
        });
        GridBagConstraints gbc_addButton = new GridBagConstraints();
        gbc_addButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_addButton.insets = new Insets(0, 0, 5, 5);
        gbc_addButton.gridx = 0;
        gbc_addButton.gridy = 0;
        listActionPanel.add(addButton, gbc_addButton);

        JButton deleteButton = new JButton("Delete Plugin");
        deleteButton.setToolTipText("Delete selected plugin from the repository");
        deleteButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final PluginIdent ident = (PluginIdent) identList.getSelectedValue();

                if (ident != null)
                {
                    if (ConfirmDialog.confirm("Remove '" + ident.getName() + "' plugin from the list ?"))
                    {
                        pluginMap.remove(ident);
                        updatePluginList();
                    }
                }
            }
        });
        GridBagConstraints gbc_deleteButton = new GridBagConstraints();
        gbc_deleteButton.gridwidth = 2;
        gbc_deleteButton.insets = new Insets(0, 0, 5, 0);
        gbc_deleteButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_deleteButton.gridx = 1;
        gbc_deleteButton.gridy = 0;
        listActionPanel.add(deleteButton, gbc_deleteButton);

        localPluginsComboBox = new JComboBox();
        localPluginsComboBox.setToolTipText("Select a plugin and add it to the repository with the '+' button");
        GridBagConstraints gbc_localPluginsComboBox = new GridBagConstraints();
        gbc_localPluginsComboBox.gridwidth = 2;
        gbc_localPluginsComboBox.insets = new Insets(0, 0, 0, 5);
        gbc_localPluginsComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_localPluginsComboBox.gridx = 0;
        gbc_localPluginsComboBox.gridy = 1;
        listActionPanel.add(localPluginsComboBox, gbc_localPluginsComboBox);

        final IcyButton addLocalPluginButton = new IcyButton(new IcyIcon(ResourceUtil.ICON_PLUS));
        addLocalPluginButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final PluginDescriptor plugin = getSelectedLocalPlugin();

                if (plugin != null)
                {
                    pluginMap.put(createIdent(plugin), plugin);
                    updatePluginList();
                }
            }
        });
        addLocalPluginButton.setFlat(true);
        addLocalPluginButton.setToolTipText("Add selected plugin in the repository");
        GridBagConstraints gbc_addLocalPluginButton = new GridBagConstraints();
        gbc_addLocalPluginButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_addLocalPluginButton.gridx = 2;
        gbc_addLocalPluginButton.gridy = 1;
        listActionPanel.add(addLocalPluginButton, gbc_addLocalPluginButton);

        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

        final JSplitPane splitPane = new JSplitPane();
        add(splitPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(listPanel);
        splitPane.setRightComponent(tabbedPane);

        JPanel pluginDetailsPanel = new JPanel();
        tabbedPane.addTab("Plugin Properties", null, pluginDetailsPanel, null);

        GridBagLayout gbl_pluginDetailsPanel = new GridBagLayout();
        gbl_pluginDetailsPanel.columnWidths = new int[] {0, 0, 0};
        gbl_pluginDetailsPanel.rowHeights = new int[] {0, 0, 0, 0, 20, 0, 0, 0, 0, 20, 0, 0};
        gbl_pluginDetailsPanel.columnWeights = new double[] {0.0, 1.0, Double.MIN_VALUE};
        gbl_pluginDetailsPanel.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                Double.MIN_VALUE};
        pluginDetailsPanel.setLayout(gbl_pluginDetailsPanel);

        JLabel lblName = new JLabel("Name");
        GridBagConstraints gbc_lblName = new GridBagConstraints();
        gbc_lblName.insets = new Insets(0, 0, 5, 5);
        gbc_lblName.anchor = GridBagConstraints.EAST;
        gbc_lblName.gridx = 0;
        gbc_lblName.gridy = 0;
        pluginDetailsPanel.add(lblName, gbc_lblName);

        pluginNameField = new IcyTextField();
        pluginNameField.setEnabled(false);
        pluginNameField.setToolTipText("Plugin name");
        GridBagConstraints gbc_pluginNameField = new GridBagConstraints();
        gbc_pluginNameField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginNameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginNameField.gridx = 1;
        gbc_pluginNameField.gridy = 0;
        pluginDetailsPanel.add(pluginNameField, gbc_pluginNameField);
        pluginNameField.setColumns(10);

        JLabel lblNewLabel = new JLabel("Version");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 1;
        pluginDetailsPanel.add(lblNewLabel, gbc_lblNewLabel);

        pluginVersionField = new IcyTextField();
        pluginVersionField.setEnabled(false);
        pluginVersionField.setToolTipText("Plugin version information");
        GridBagConstraints gbc_pluginVersionField = new GridBagConstraints();
        gbc_pluginVersionField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginVersionField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginVersionField.gridx = 1;
        gbc_pluginVersionField.gridy = 1;
        pluginDetailsPanel.add(pluginVersionField, gbc_pluginVersionField);
        pluginVersionField.setColumns(10);

        JLabel lblMinimumKernelVersion = new JLabel("Minimum kernel version");
        GridBagConstraints gbc_lblMinimumKernelVersion = new GridBagConstraints();
        gbc_lblMinimumKernelVersion.anchor = GridBagConstraints.EAST;
        gbc_lblMinimumKernelVersion.insets = new Insets(0, 0, 5, 5);
        gbc_lblMinimumKernelVersion.gridx = 0;
        gbc_lblMinimumKernelVersion.gridy = 2;
        pluginDetailsPanel.add(lblMinimumKernelVersion, gbc_lblMinimumKernelVersion);

        pluginMinKernelVerField = new IcyTextField();
        pluginMinKernelVerField.setEnabled(false);
        pluginMinKernelVerField.setToolTipText("Minimum kernel version required for this plugin");
        GridBagConstraints gbc_pluginMinKernelVerField = new GridBagConstraints();
        gbc_pluginMinKernelVerField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginMinKernelVerField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginMinKernelVerField.gridx = 1;
        gbc_pluginMinKernelVerField.gridy = 2;
        pluginDetailsPanel.add(pluginMinKernelVerField, gbc_pluginMinKernelVerField);
        pluginMinKernelVerField.setColumns(10);

        JLabel lblFileNameno = new JLabel("Class name");
        GridBagConstraints gbc_lblFileNameno = new GridBagConstraints();
        gbc_lblFileNameno.anchor = GridBagConstraints.EAST;
        gbc_lblFileNameno.insets = new Insets(0, 0, 5, 5);
        gbc_lblFileNameno.gridx = 0;
        gbc_lblFileNameno.gridy = 3;
        pluginDetailsPanel.add(lblFileNameno, gbc_lblFileNameno);

        pluginClassNameField = new IcyTextField();
        pluginClassNameField.setEnabled(false);
        pluginClassNameField.setToolTipText("Plugin class name (should be \"plugins.author.xxx\" format)");
        GridBagConstraints gbc_pluginClassNameField = new GridBagConstraints();
        gbc_pluginClassNameField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginClassNameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginClassNameField.gridx = 1;
        gbc_pluginClassNameField.gridy = 3;
        pluginDetailsPanel.add(pluginClassNameField, gbc_pluginClassNameField);
        pluginClassNameField.setColumns(10);

        JLabel lblDescription = new JLabel("Description");
        GridBagConstraints gbc_lblDescription = new GridBagConstraints();
        gbc_lblDescription.fill = GridBagConstraints.VERTICAL;
        gbc_lblDescription.anchor = GridBagConstraints.EAST;
        gbc_lblDescription.insets = new Insets(0, 0, 5, 5);
        gbc_lblDescription.gridx = 0;
        gbc_lblDescription.gridy = 4;
        pluginDetailsPanel.add(lblDescription, gbc_lblDescription);

        final JScrollPane scrollPane_2 = new JScrollPane();
        scrollPane_2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane_2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
        gbc_scrollPane_2.gridheight = 2;
        gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_2.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_2.gridx = 1;
        gbc_scrollPane_2.gridy = 4;
        pluginDetailsPanel.add(scrollPane_2, gbc_scrollPane_2);

        pluginDescriptionField = new JTextArea();
        pluginDescriptionField.setEnabled(false);
        pluginDescriptionField.setToolTipText("Plugin description");
        scrollPane_2.setViewportView(pluginDescriptionField);
        pluginDescriptionField.setRows(2);

        JLabel lblAuthor = new JLabel("Author");
        GridBagConstraints gbc_lblAuthor = new GridBagConstraints();
        gbc_lblAuthor.anchor = GridBagConstraints.EAST;
        gbc_lblAuthor.insets = new Insets(0, 0, 5, 5);
        gbc_lblAuthor.gridx = 0;
        gbc_lblAuthor.gridy = 6;
        pluginDetailsPanel.add(lblAuthor, gbc_lblAuthor);

        pluginAuthorField = new IcyTextField();
        pluginAuthorField.setEnabled(false);
        pluginAuthorField.setToolTipText("Plugin author name");
        GridBagConstraints gbc_pluginAuthorField = new GridBagConstraints();
        gbc_pluginAuthorField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginAuthorField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginAuthorField.gridx = 1;
        gbc_pluginAuthorField.gridy = 6;
        pluginDetailsPanel.add(pluginAuthorField, gbc_pluginAuthorField);
        pluginAuthorField.setColumns(10);

        final JLabel lblEmail = new JLabel("Email");
        GridBagConstraints gbc_lblEmail = new GridBagConstraints();
        gbc_lblEmail.anchor = GridBagConstraints.EAST;
        gbc_lblEmail.insets = new Insets(0, 0, 5, 5);
        gbc_lblEmail.gridx = 0;
        gbc_lblEmail.gridy = 7;
        pluginDetailsPanel.add(lblEmail, gbc_lblEmail);

        pluginEmailField = new IcyTextField();
        pluginEmailField.setEnabled(false);
        pluginEmailField.setToolTipText("Contact email");
        GridBagConstraints gbc_pluginEmailField = new GridBagConstraints();
        gbc_pluginEmailField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginEmailField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginEmailField.gridx = 1;
        gbc_pluginEmailField.gridy = 7;
        pluginDetailsPanel.add(pluginEmailField, gbc_pluginEmailField);
        pluginEmailField.setColumns(10);

        JLabel lblWebsite = new JLabel("Website");
        GridBagConstraints gbc_lblWebsite = new GridBagConstraints();
        gbc_lblWebsite.anchor = GridBagConstraints.EAST;
        gbc_lblWebsite.insets = new Insets(0, 0, 5, 5);
        gbc_lblWebsite.gridx = 0;
        gbc_lblWebsite.gridy = 8;
        pluginDetailsPanel.add(lblWebsite, gbc_lblWebsite);

        pluginWebField = new IcyTextField();
        pluginWebField.setEnabled(false);
        pluginWebField.setToolTipText("Website URL");
        GridBagConstraints gbc_pluginWebField = new GridBagConstraints();
        gbc_pluginWebField.insets = new Insets(0, 0, 5, 0);
        gbc_pluginWebField.fill = GridBagConstraints.HORIZONTAL;
        gbc_pluginWebField.gridx = 1;
        gbc_pluginWebField.gridy = 8;
        pluginDetailsPanel.add(pluginWebField, gbc_pluginWebField);
        pluginWebField.setColumns(10);

        JLabel lblDependencies = new JLabel("Dependencies");
        GridBagConstraints gbc_lblDependencies = new GridBagConstraints();
        gbc_lblDependencies.anchor = GridBagConstraints.EAST;
        gbc_lblDependencies.insets = new Insets(0, 0, 5, 5);
        gbc_lblDependencies.gridx = 0;
        gbc_lblDependencies.gridy = 9;
        pluginDetailsPanel.add(lblDependencies, gbc_lblDependencies);

        final JScrollPane scrollPane_1 = new JScrollPane();
        scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane_1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
        gbc_scrollPane_1.gridheight = 2;
        gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_1.gridx = 1;
        gbc_scrollPane_1.gridy = 9;
        pluginDetailsPanel.add(scrollPane_1, gbc_scrollPane_1);

        pluginDependenciesField = new JTextArea();
        pluginDependenciesField.setEnabled(false);
        pluginDependenciesField.setToolTipText("Plugin dependencies (should be  a list of plugin class name)");
        scrollPane_1.setViewportView(pluginDependenciesField);
        pluginDependenciesField.setRows(4);

        final JPanel fixedParamPanel = new JPanel();
        tabbedPane.addTab("Default Plugin Properties", null, fixedParamPanel, null);

        GridBagLayout gbl_fixedParamPanel = new GridBagLayout();
        gbl_fixedParamPanel.columnWidths = new int[] {0, 0, 0};
        gbl_fixedParamPanel.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
        gbl_fixedParamPanel.columnWeights = new double[] {0.0, 1.0, Double.MIN_VALUE};
        gbl_fixedParamPanel.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        fixedParamPanel.setLayout(gbl_fixedParamPanel);

        final JLabel lblNewLabel_1 = new JLabel("Version");
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel_1.gridx = 0;
        gbc_lblNewLabel_1.gridy = 0;
        fixedParamPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

        defaultPluginVersionField = new JTextField();
        defaultPluginVersionField.setToolTipText("Default plugin version");
        GridBagConstraints gbc_defaultPluginVersionField = new GridBagConstraints();
        gbc_defaultPluginVersionField.insets = new Insets(0, 0, 5, 0);
        gbc_defaultPluginVersionField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginVersionField.gridx = 1;
        gbc_defaultPluginVersionField.gridy = 0;
        fixedParamPanel.add(defaultPluginVersionField, gbc_defaultPluginVersionField);
        defaultPluginVersionField.setColumns(10);

        final JLabel lblMinimumKernelVersion_1 = new JLabel("Minimum kernel version");
        GridBagConstraints gbc_lblMinimumKernelVersion_1 = new GridBagConstraints();
        gbc_lblMinimumKernelVersion_1.anchor = GridBagConstraints.EAST;
        gbc_lblMinimumKernelVersion_1.insets = new Insets(0, 0, 5, 5);
        gbc_lblMinimumKernelVersion_1.gridx = 0;
        gbc_lblMinimumKernelVersion_1.gridy = 1;
        fixedParamPanel.add(lblMinimumKernelVersion_1, gbc_lblMinimumKernelVersion_1);

        defaultPluginMinKernelVerField = new JTextField();
        defaultPluginMinKernelVerField.setToolTipText("Default minimum kernel version required for this plugin");
        GridBagConstraints gbc_defaultPluginMinKernelVerField = new GridBagConstraints();
        gbc_defaultPluginMinKernelVerField.insets = new Insets(0, 0, 5, 0);
        gbc_defaultPluginMinKernelVerField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginMinKernelVerField.gridx = 1;
        gbc_defaultPluginMinKernelVerField.gridy = 1;
        fixedParamPanel.add(defaultPluginMinKernelVerField, gbc_defaultPluginMinKernelVerField);
        defaultPluginMinKernelVerField.setColumns(10);

        final JLabel lblClassName = new JLabel("Class name");
        GridBagConstraints gbc_lblClassName = new GridBagConstraints();
        gbc_lblClassName.anchor = GridBagConstraints.EAST;
        gbc_lblClassName.insets = new Insets(0, 0, 5, 5);
        gbc_lblClassName.gridx = 0;
        gbc_lblClassName.gridy = 2;
        fixedParamPanel.add(lblClassName, gbc_lblClassName);

        defaultPluginClassNameField = new JTextField();
        defaultPluginClassNameField
                .setToolTipText("Default plugin class name (should be \"plugins.author.xxx\" format)");
        GridBagConstraints gbc_defaultPluginClassNameField = new GridBagConstraints();
        gbc_defaultPluginClassNameField.insets = new Insets(0, 0, 5, 0);
        gbc_defaultPluginClassNameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginClassNameField.gridx = 1;
        gbc_defaultPluginClassNameField.gridy = 2;
        fixedParamPanel.add(defaultPluginClassNameField, gbc_defaultPluginClassNameField);
        defaultPluginClassNameField.setColumns(10);

        final JLabel lblAuthor_1 = new JLabel("Author");
        GridBagConstraints gbc_lblAuthor_1 = new GridBagConstraints();
        gbc_lblAuthor_1.anchor = GridBagConstraints.EAST;
        gbc_lblAuthor_1.insets = new Insets(0, 0, 5, 5);
        gbc_lblAuthor_1.gridx = 0;
        gbc_lblAuthor_1.gridy = 3;
        fixedParamPanel.add(lblAuthor_1, gbc_lblAuthor_1);

        defaultPluginAuthorField = new JTextField();
        defaultPluginAuthorField.setToolTipText("Default plugin author name");
        GridBagConstraints gbc_defaultPluginAuthorField = new GridBagConstraints();
        gbc_defaultPluginAuthorField.insets = new Insets(0, 0, 5, 0);
        gbc_defaultPluginAuthorField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginAuthorField.gridx = 1;
        gbc_defaultPluginAuthorField.gridy = 3;
        fixedParamPanel.add(defaultPluginAuthorField, gbc_defaultPluginAuthorField);
        defaultPluginAuthorField.setColumns(10);

        final JLabel lblNewLabel_2 = new JLabel("Email");
        GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
        gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel_2.gridx = 0;
        gbc_lblNewLabel_2.gridy = 4;
        fixedParamPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

        defaultPluginEmailField = new JTextField();
        defaultPluginEmailField.setToolTipText("Default contact email");
        GridBagConstraints gbc_defaultPluginEmailField = new GridBagConstraints();
        gbc_defaultPluginEmailField.insets = new Insets(0, 0, 5, 0);
        gbc_defaultPluginEmailField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginEmailField.gridx = 1;
        gbc_defaultPluginEmailField.gridy = 4;
        fixedParamPanel.add(defaultPluginEmailField, gbc_defaultPluginEmailField);
        defaultPluginEmailField.setColumns(10);

        final JLabel lblWebsite_1 = new JLabel("Website");
        GridBagConstraints gbc_lblWebsite_1 = new GridBagConstraints();
        gbc_lblWebsite_1.anchor = GridBagConstraints.EAST;
        gbc_lblWebsite_1.insets = new Insets(0, 0, 0, 5);
        gbc_lblWebsite_1.gridx = 0;
        gbc_lblWebsite_1.gridy = 5;
        fixedParamPanel.add(lblWebsite_1, gbc_lblWebsite_1);

        defaultPluginWebField = new JTextField();
        defaultPluginWebField.setToolTipText("Default website URL");
        GridBagConstraints gbc_defaultPluginWebField = new GridBagConstraints();
        gbc_defaultPluginWebField.fill = GridBagConstraints.HORIZONTAL;
        gbc_defaultPluginWebField.gridx = 1;
        gbc_defaultPluginWebField.gridy = 5;
        fixedParamPanel.add(defaultPluginWebField, gbc_defaultPluginWebField);
        defaultPluginWebField.setColumns(10);
    }

    String getBaseLocalPath()
    {
        return FileUtil.getDirectory(pathLabel.getText());
    }

    String getBaseOnlinePath()
    {
        return baseURLField.getText();
    }

    private void updateOnlinePath()
    {
        // extract base online path from xml url info
        final PluginIdent ident = (PluginIdent) identList.getSelectedValue();

        if (ident != null)
        {
            String text = FileUtil.getDirectory(ident.getUrlInternal());
            text = StringUtil.removeLast(text, ClassUtil.getPackageName(ident.getClassName()).length() + 1);
            baseURLField.setText(text);
        }
    }

    private void updateLocalPluginsList()
    {
        final int saveInd = Math.max(identList.getSelectedIndex(), 0);

        // update list of local plugins
        localPluginsComboBox.setModel(new LocalPluginComboBoxModel(PluginLoader.getPlugins()));
        if (localPluginsComboBox.getModel().getSize() > saveInd)
            localPluginsComboBox.setSelectedIndex(saveInd);
        else
            localPluginsComboBox.setSelectedIndex(localPluginsComboBox.getModel().getSize());
    }

    void loadXMLFile(String path)
    {
        final List<PluginIdent> result = new ArrayList<PluginIdent>();

        final Document document = XMLUtil.loadDocument(path);
        // get plugins node
        final Node pluginsNode = XMLUtil.getElement(document.getDocumentElement(), "plugins");

        // plugins node found
        if (pluginsNode != null)
        {
            // ident nodes
            final List<Node> nodes = XMLUtil.getChildren(pluginsNode, "plugin");

            for (Node node : nodes)
            {
                final PluginIdent ident = new PluginIdent();

                ident.loadFromXML(node);

                // accept only if not empty
                if (!ident.isEmpty())
                    result.add(ident);
            }

            // update path information
            pathLabel.setText(path);

            // rebuild plugin map
            pluginMap.clear();
            for (PluginIdent ident : result)
                // load plugin descriptor from file
                pluginMap.put(ident, loadPluginDescriptor(ident));

            // update plugin list
            updatePluginList();

            // extract base online path from xml url info
            updateOnlinePath();
        }
    }

    void saveXMLFile(String path)
    {
        final Document document = XMLUtil.createDocument(true);
        final Element root = XMLUtil.getRootElement(document);
        // get plugins node
        final Element pluginsNode = XMLUtil.setElement(root, "plugins");

        // update path information before saving XML as they use this information
        pathLabel.setText(path);

        for (Entry<PluginIdent, PluginDescriptor> entry : pluginMap.entrySet())
        {
            final PluginIdent ident = entry.getKey();

            // save ident in a new node
            if (!ident.isEmpty())
            {
                ident.saveToXML(XMLUtil.addElement(pluginsNode, "plugin"));
                // save plugin descriptor to file
                entry.getValue().saveToXML();
            }
        }

        XMLUtil.saveDocument(document, path);
    }

    protected PluginDescriptor getSelectedLocalPlugin()
    {
        return (PluginDescriptor) localPluginsComboBox.getSelectedItem();
    }

    void updatePluginList()
    {
        final int saveInd = Math.max(identList.getSelectedIndex(), 0);

        // set list model
        identList.setModel(new IdentListModel(new ArrayList<PluginIdent>(pluginMap.keySet())));
        if (identList.getModel().getSize() > saveInd)
            identList.setSelectedIndex(saveInd);
        else
            identList.setSelectedIndex(identList.getModel().getSize() - 1);
    }

    PluginDescriptor loadPluginDescriptor(PluginIdent ident)
    {
        final PluginDescriptor result = new PluginDescriptor();
        final Document document = XMLUtil.loadDocument(getBaseLocalPath()
                + ClassUtil.getPathFromQualifiedName(ident.getClassName()) + XMLUtil.FILE_DOT_EXTENSION);

        if (document == null)
            return null;

        if (!result.loadFromXML(document.getDocumentElement()))
            return null;

        // restore class name from ident
        result.setClassName(ident.getClassName());

        return result;
    }

    PluginIdent createIdent(PluginDescriptor plugin)
    {
        final PluginIdent result = new PluginIdent();

        setIdent(result, plugin);

        return result;
    }

    PluginDescriptor createPlugin()
    {
        final PluginDescriptor result = new PluginDescriptor();

        result.setName("My Plugin " + pluginId++);
        result.setClassName(getDefaultPluginClassName());
        result.setAuthor(getDefaultPluginAuthor());
        result.setDescription("Plugin description");
        result.setVersion(new Version(getDefaultPluginVersion()));
        result.setRequiredKernelVersion(new Version(getDefaultPluginMinKernelVersion()));
        result.setEmail(getDefaultPluginEmail());
        result.setWeb(getDefaultPluginWeb());

        return result;
    }

    void getPluginDetails(PluginDescriptor plugin)
    {
        if (plugin == null)
            return;

        plugin.setName(pluginNameField.getText());
        plugin.setClassName(pluginClassNameField.getText());
        plugin.setAuthor(pluginAuthorField.getText());
        plugin.setDescription(pluginDescriptionField.getText());
        plugin.setVersion(new Version(pluginVersionField.getText()));
        plugin.setRequiredKernelVersion(new Version(pluginMinKernelVerField.getText()));
        plugin.setEmail(pluginEmailField.getText());
        plugin.setWeb(pluginWebField.getText());
        plugin.setDependenciesAsString(pluginDependenciesField.getText());
    }

    void updatePlugin()
    {
        getPluginDetails(currentPlugin);
        setIdent(currentIdent, currentPlugin);
        if (identList.getModel() instanceof IdentListModel)
            ((IdentListModel) identList.getModel()).contentsChanged();
    }

    void setIdent(PluginIdent ident, PluginDescriptor plugin)
    {
        if ((ident == null) || (plugin == null))
            return;

        ident.setClassName(plugin.getClassName());
        ident.setVersion(plugin.getVersion());
        ident.setRequiredKernelVersion(plugin.getRequiredKernelVersion());
        ident.setName(plugin.getName());
    }

    private void setPluginDetails(PluginDescriptor plugin)
    {
        if (plugin != null)
        {
            pluginNameField.setEnabled(true);
            pluginClassNameField.setEnabled(true);
            pluginAuthorField.setEnabled(true);
            pluginDescriptionField.setEnabled(true);
            pluginVersionField.setEnabled(true);
            pluginMinKernelVerField.setEnabled(true);
            pluginEmailField.setEnabled(true);
            pluginWebField.setEnabled(true);
            pluginDependenciesField.setEnabled(true);

            adjustingFields = true;
            try
            {
                if (plugin.getName() != pluginNameField.getText())
                    pluginNameField.setText(plugin.getName());
                if (plugin.getClassName() != pluginClassNameField.getText())
                    pluginClassNameField.setText(plugin.getClassName());
                if (plugin.getAuthor() != pluginAuthorField.getText())
                    pluginAuthorField.setText(plugin.getAuthor());
                if (plugin.getDescription() != pluginDescriptionField.getText())
                    pluginDescriptionField.setText(plugin.getDescription());
                if (plugin.getVersion().toString() != pluginVersionField.getText())
                    pluginVersionField.setText(plugin.getVersion().toString());
                if (plugin.getRequiredKernelVersion().toString() != pluginMinKernelVerField.getText())
                    pluginMinKernelVerField.setText(plugin.getRequiredKernelVersion().toString());
                if (plugin.getEmail() != pluginEmailField.getText())
                    pluginEmailField.setText(plugin.getEmail());
                if (plugin.getWeb() != pluginWebField.getText())
                    pluginWebField.setText(plugin.getWeb());
                if (plugin.getDependenciesAsString() != pluginDependenciesField.getText())
                    pluginDependenciesField.setText(plugin.getDependenciesAsString());
            }
            finally
            {
                adjustingFields = false;
            }
        }
        else
        {
            pluginNameField.setText("");
            pluginClassNameField.setText("");
            pluginAuthorField.setText("");
            pluginDescriptionField.setText("");
            pluginVersionField.setText("");
            pluginMinKernelVerField.setText("");
            pluginEmailField.setText("");
            pluginWebField.setText("");
            pluginDependenciesField.setText("");

            pluginNameField.setEnabled(false);
            pluginClassNameField.setEnabled(false);
            pluginAuthorField.setEnabled(false);
            pluginDescriptionField.setEnabled(false);
            pluginVersionField.setEnabled(false);
            pluginMinKernelVerField.setEnabled(false);
            pluginEmailField.setEnabled(false);
            pluginWebField.setEnabled(false);
            pluginDependenciesField.setEnabled(false);
        }
    }

    private String getDefaultPluginClassName()
    {
        return defaultPluginClassNameField.getText();
    }

    private String getDefaultPluginVersion()
    {
        return defaultPluginVersionField.getText();
    }

    private String getDefaultPluginMinKernelVersion()
    {
        return defaultPluginMinKernelVerField.getText();
    }

    private String getDefaultPluginAuthor()
    {
        return defaultPluginAuthorField.getText();
    }

    private String getDefaultPluginEmail()
    {
        return defaultPluginEmailField.getText();
    }

    private String getDefaultPluginWeb()
    {
        return defaultPluginWebField.getText();
    }

    private void setDefaultPluginClassName(String value)
    {
        defaultPluginClassNameField.setText(value);
    }

    private void setDefaultPluginVersion(String value)
    {
        defaultPluginVersionField.setText(value);
    }

    private void setDefaultPluginMinKernelVersion(String value)
    {
        defaultPluginMinKernelVerField.setText(value);
    }

    private void setDefaultPluginAuthor(String value)
    {
        defaultPluginAuthorField.setText(value);
    }

    private void setDefaultPluginEmail(String value)
    {
        defaultPluginEmailField.setText(value);
    }

    private void setDefaultPluginWeb(String value)
    {
        defaultPluginWebField.setText(value);
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting())
            return;

        // update current ident & plugin
        currentIdent = (PluginIdent) identList.getSelectedValue();
        if (currentIdent != null)
            currentPlugin = pluginMap.get(currentIdent);
        else
            currentPlugin = null;

        setPluginDetails(currentPlugin);
    }

    @Override
    public void textChanged(IcyTextField source, boolean validate)
    {
        if (adjustingFields)
            return;

        if (validate)
            updatePlugin();
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        if (adjustingFields)
            return;

        updatePlugin();
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        if (adjustingFields)
            return;

        updatePlugin();
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        if (adjustingFields)
            return;

        updatePlugin();
    }

    @Override
    public void pluginLoaderChanged(PluginLoaderEvent e)
    {
        updateLocalPluginsList();
    }

    public void loadParams(XMLPreferences prefs)
    {
        setDefaultPluginVersion(prefs.get(ID_DEFAULT_VERSION, "1.0.0.0"));
        setDefaultPluginClassName(prefs.get(ID_DEFAULT_CLASSNAME, "plugins.author.package.MyPlugin"));
        setDefaultPluginMinKernelVersion(prefs.get(ID_DEFAULT_REQUIRED_KERNEL_VERSION, "1.4.0.0"));
        setDefaultPluginAuthor(prefs.get(ID_DEFAULT_AUTHOR, ""));
        setDefaultPluginEmail(prefs.get(ID_DEFAULT_EMAIL, ""));
        setDefaultPluginWeb(prefs.get(ID_DEFAULT_WEB, ""));
    }

    public void saveParams(XMLPreferences prefs)
    {
        prefs.put(ID_DEFAULT_VERSION, getDefaultPluginVersion());
        prefs.put(ID_DEFAULT_CLASSNAME, getDefaultPluginClassName());
        prefs.put(ID_DEFAULT_REQUIRED_KERNEL_VERSION, getDefaultPluginMinKernelVersion());
        prefs.put(ID_DEFAULT_AUTHOR, getDefaultPluginAuthor());
        prefs.put(ID_DEFAULT_EMAIL, getDefaultPluginEmail());
        prefs.put(ID_DEFAULT_WEB, getDefaultPluginWeb());
    }
}
