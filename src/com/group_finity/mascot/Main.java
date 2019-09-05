package com.group_finity.mascot;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.group_finity.mascot.win.WindowsInteractiveWindowForm;
import com.joconner.i18n.Utf8ResourceBundleControl;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODMain;
import com.nilo.plaf.nimrod.NimRODTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.UIManager;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Program entry point.
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Main
{
    private static final Logger log = Logger.getLogger( Main.class.getName() );
    // Action that matches the "Gather Around Mouse!" context menu command
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static
    {
        try
        {
            LogManager.getLogManager( ).readConfiguration( Main.class.getResourceAsStream( "/logging.properties" ) );
        }
        catch( final SecurityException e )
        {
            e.printStackTrace( );
        }
        catch( final IOException e )
        {
            e.printStackTrace( );
        }
        catch( OutOfMemoryError err )
        {
            log.log( Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err );
            Main.showError( "Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again." );
            System.exit( 0 );
        }
    }
    private final Manager manager = new Manager( );
    private ArrayList<String> imageSets = new ArrayList<String>( );
    private Hashtable<String, Configuration> configurations = new Hashtable<String, Configuration>( );
    private static Main instance = new Main( );
    private Properties properties = new Properties( );
    private Platform platform;
    private ResourceBundle languageBundle;
    
    private JDialog form;
    
    public static Main getInstance( )
    {
        return instance;
    }
    private static JFrame frame = new javax.swing.JFrame( );

    public static void showError( String message )
    {
        JOptionPane.showMessageDialog( frame, message, "Error", JOptionPane.ERROR_MESSAGE );
    }

    public static void main( final String[] args )
    {
        try
        {
            getInstance( ).run( );
        }
        catch( OutOfMemoryError err )
        {
            log.log( Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err );
            Main.showError( "Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again." );
            System.exit( 0 );
        }
    }

    public void run( )
    {
        // test operating system
        if( !System.getProperty("sun.arch.data.model").equals( "64" ) )
            platform = Platform.x86;
        else
            platform = Platform.x86_64;
        
        // load properties
        properties = new Properties( );
        FileInputStream input;
        try
        {
            input = new FileInputStream( "./conf/settings.properties" );
            properties.load( input );
        }
        catch( FileNotFoundException ex )
        {
        }
        catch( IOException ex )
        {
        }
        
        // load langauges
        try   
        {
            ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl( false );
            languageBundle = ResourceBundle.getBundle( "language", Locale.forLanguageTag( properties.getProperty( "Language", "en-GB" ) ), utf8Control );
        }
        catch( Exception ex )
        {
            Main.showError( "The default language file could not be loaded. Ensure that you have the latest shimeji language.properties in your conf directory." );
            exit( );
        }
        
        // load theme
        try
        {
            // default light theme
            NimRODLookAndFeel lookAndFeel = new NimRODLookAndFeel( );
            
            // check for theme properties
            NimRODTheme theme = null;
            try
            {
                if( new File( "./conf/theme.properties" ).isFile( ) )
                {
                    theme = new NimRODTheme( "./conf/theme.properties" );
                }
            }
            catch( Exception exc )
            {
                theme = null;
            }
            
            if( theme == null )
            {
                // default back to light theme if not found/valid
                theme = new NimRODTheme( );
                theme.setPrimary1( Color.decode( "#1EA6EB" ) );
                theme.setPrimary2( Color.decode( "#28B0F5" ) );
                theme.setPrimary3( Color.decode( "#32BAFF" ) );
                theme.setSecondary1( Color.decode( "#BCBCBE" ) );
                theme.setSecondary2( Color.decode( "#C6C6C8" ) );
                theme.setSecondary3( Color.decode( "#D0D0D2" ) );
                theme.setMenuOpacity( 255 );
                theme.setFrameOpacity( 255 );
            }
            
            // handle menu size
            if( !properties.containsKey( "MenuDPI" ) )
            {
                int dpi = java.awt.Toolkit.getDefaultToolkit( ).getScreenResolution( );
                if( dpi < 96 )
                    dpi = 96;
                properties.setProperty( "MenuDPI", dpi + "" );
                try
                {
                    FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                    try
                    {
                        properties.store( output, "Shimeji-ee Configuration Options" );
                    }
                    finally
                    {
                        output.close( );
                    }
                }
                catch( Exception unimportant )
                {
                }
            }
            float menuScaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;
            java.awt.Font font = theme.getUserTextFont( ).deriveFont( theme.getUserTextFont( ).getSize( ) * menuScaling );
            theme.setFont( font );
            
            NimRODLookAndFeel.setCurrentTheme( theme );
            JFrame.setDefaultLookAndFeelDecorated( true );
            JDialog.setDefaultLookAndFeelDecorated( true );
            // all done
            lookAndFeel.initialize( );
            UIManager.setLookAndFeel( lookAndFeel );
        }
        catch( Exception ex )
        {
            try
            {
                UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName( ) );
            }
            catch( Exception ex1 )
            {
                log.log( Level.SEVERE, "Look & Feel unsupported.", ex1 );
                exit( );
            }
        }
        
        // Get the image sets to use
        imageSets.addAll( Arrays.asList( properties.getProperty( "ActiveShimeji", "" ).split( "/" ) ) );
        if( imageSets.get( 0 ).trim( ).isEmpty( ) )
        {
            imageSets = new ImageSetChooser( frame, true ).display( );
            if( imageSets == null )
            {
                exit( );
            }
        }

        // Load settings
        for( int index = 0; index < imageSets.size( ); index++ )
        {
            if( loadConfiguration( imageSets.get( index ) ) == false )
            {
                // failed validation
                configurations.remove( imageSets.get( index ) );
                imageSets.remove( imageSets.get( index ) );
                index--;
            }
        }
        if( imageSets.isEmpty( ) )
        {
            exit( );
        }

        // Create the tray icon
        createTrayIcon( );

        // Create the first mascot
        for( String imageSet : imageSets )
        {
            createMascot( imageSet );
        }

        getManager( ).start( );
    }

    private boolean loadConfiguration( final String imageSet )
    {
        try
        {
            String actionsFile = "./conf/actions.xml";
            if( new File( "./conf/" + imageSet + "/actions.xml" ).exists() )
            {
                actionsFile = "./conf/" + imageSet + "/actions.xml";
            }
            else if( new File( "./img/" + imageSet + "/conf/actions.xml" ).exists() )
            {
                actionsFile = "./img/" + imageSet + "/conf/actions.xml";
            }

            log.log( Level.INFO, imageSet + " Read Action File ({0})", actionsFile );

            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new FileInputStream( new File( actionsFile ) ) );

            Configuration configuration = new Configuration();

            configuration.load( new Entry( actions.getDocumentElement( ) ), imageSet );

            String behaviorsFile = "./conf/behaviors.xml";
            if( new File( "./conf/" + imageSet + "/behaviors.xml" ).exists() )
            {
                behaviorsFile = "./conf/" + imageSet + "/behaviors.xml";
            }
            else if( new File( "./img/" + imageSet + "/conf/behaviors.xml" ).exists() )
            {
                behaviorsFile = "./img/" + imageSet + "/conf/behaviors.xml";
            }

            log.log( Level.INFO, imageSet + " Read Behavior File ({0})", behaviorsFile );

            final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new FileInputStream( new File( behaviorsFile ) ) );

            configuration.load( new Entry( behaviors.getDocumentElement() ), imageSet );

            configuration.validate();

            configurations.put( imageSet, configuration );
            
            // born mascot bit goes here...
            for( final Entry list : new Entry( actions.getDocumentElement( ) ).selectChildren( "ActionList" ) )
            {
                for( final Entry node : list.selectChildren( "Action" ) )
                {
                    if( node.getAttributes( ).containsKey( "BornMascot" ) )
                    {
                        if( ! configurations.containsKey( node.getAttribute( "BornMascot" ) ) )
                        {
                            loadConfiguration( node.getAttribute( "BornMascot" ) );
                        }
                    }
                    if( node.getAttributes( ).containsKey( "TransformMascot" ) )
                    {
                        if( ! configurations.containsKey( node.getAttribute( "TransformMascot" ) ) )
                        {
                            loadConfiguration( node.getAttribute( "TransformMascot" ) );
                        }
                    }
                }
            }

            return true;
        }
        catch( final IOException e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
        }
        catch( final SAXException e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
        }
            catch( final ParserConfigurationException e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
        }
        catch( final ConfigurationException e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
        }
        catch( final Exception e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
        }
        
        return false;
    }

    /**
     * Create a tray icon.
     *
     * @ Throws AWTException
     * @ Throws IOException
     */
    private void createTrayIcon()
    {
        log.log( Level.INFO, "create a tray icon" );
        
        try
        {
            // Create the tray icon
            final TrayIcon icon = new TrayIcon( ImageIO.read( Main.class.getResource( "/icon.png" ) ), languageBundle.getString( "ShimejiEE" ) );
            
            // attach menu
            icon.addMouseListener( new MouseListener( )
            {
                @Override
                public void mouseClicked( MouseEvent event )
                {
                }

                @Override
                public void mousePressed( MouseEvent e )
                {
                }

                @Override
                public void mouseReleased( MouseEvent event )
                {
                    if( event.isPopupTrigger( ) )
                    {
                        // close the form if it's open
                        if( form != null )
                            form.dispose( );
                        
                        // create the form and border
                        form = new JDialog( frame, false );
                        final JPanel panel = new JPanel( );
                        panel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
                        form.add( panel );
        
                        // buttons and action handling
                        JButton btnCallShimeji = new JButton( languageBundle.getString( "CallShimeji" ) );
                        btnCallShimeji.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                createMascot( );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnFollowCursor = new JButton( languageBundle.getString( "FollowCursor" ) );
                        btnFollowCursor.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                getManager( ).setBehaviorAll( BEHAVIOR_GATHER );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnReduceToOne = new JButton( languageBundle.getString( "ReduceToOne" ) );
                        btnReduceToOne.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                getManager( ).remainOne( );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnRestoreWindows = new JButton( languageBundle.getString( "RestoreWindows" ) );
                        btnRestoreWindows.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                NativeFactory.getInstance( ).getEnvironment( ).restoreIE( );
                                form.dispose( );
                            }
                        } );
                        
                        final JButton btnAllowedBehaviours = new JButton( languageBundle.getString( "AllowedBehaviours" ) );
                        btnAllowedBehaviours.addMouseListener( new MouseListener( )
                        {
                            @Override
                            public void mouseClicked( MouseEvent e )
                            {
                            }

                            @Override
                            public void mousePressed( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseReleased( MouseEvent e )
                            {
                                btnAllowedBehaviours.setEnabled( true );
                            }

                            @Override
                            public void mouseEntered( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseExited( MouseEvent e )
                            {
                            }
                        } );
                        btnAllowedBehaviours.addActionListener( new ActionListener( )
                        {
                            @Override
                            public void actionPerformed( final ActionEvent event )
                            {
                                // "Disable Breeding" menu item
                                final JCheckBoxMenuItem breedingMenu = new JCheckBoxMenuItem( languageBundle.getString( "BreedingCloning" ), Boolean.parseBoolean( properties.getProperty( "Breeding", "true" ) ) );
                                breedingMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        if( Boolean.parseBoolean( properties.getProperty( "Breeding", "true" ) ) )
                                        {
                                            breedingMenu.setState( false );
                                            properties.setProperty( "Breeding", "false" );
                                        }
                                        else
                                        {
                                            breedingMenu.setState( true );
                                            properties.setProperty( "Breeding", "true" );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );

                                // "Throwing Windows" menu item
                                final JCheckBoxMenuItem throwingMenu = new JCheckBoxMenuItem( languageBundle.getString( "ThrowingWindows" ), Boolean.parseBoolean( properties.getProperty( "Throwing", "true" ) ) );
                                throwingMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        if( Boolean.parseBoolean( properties.getProperty( "Throwing", "true" ) ) )
                                        {
                                            throwingMenu.setState( false );
                                            properties.setProperty( "Throwing", "false" );
                                        }
                                        else
                                        {
                                            throwingMenu.setState( true );
                                            properties.setProperty( "Throwing", "true" );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );

                                // "Mute Sounds" menu item
                                final JCheckBoxMenuItem soundsMenu = new JCheckBoxMenuItem( languageBundle.getString( "SoundEffects" ), Boolean.parseBoolean( properties.getProperty( "Sounds", "true" ) ) );
                                soundsMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        if( Boolean.parseBoolean( properties.getProperty( "Sounds", "true" ) ) )
                                        {
                                            soundsMenu.setState( false );
                                            properties.setProperty( "Sounds", "false" );
                                            Sounds.setMuted( true );
                                        }
                                        else
                                        {
                                            soundsMenu.setState( true );
                                            properties.setProperty( "Sounds", "true" );
                                            Sounds.setMuted( false );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );
                                
                                JPopupMenu behaviourPopup = new JPopupMenu( );
                                behaviourPopup.add( breedingMenu );
                                behaviourPopup.add( throwingMenu );
                                behaviourPopup.add( soundsMenu );
                                behaviourPopup.addPopupMenuListener( new PopupMenuListener( )
                                {
                                    @Override
                                    public void popupMenuWillBecomeVisible( PopupMenuEvent e )
                                    {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
                                    {
                                        if( panel.getMousePosition( ) != null )
                                        {
                                            btnAllowedBehaviours.setEnabled( !( panel.getMousePosition( ).x > btnAllowedBehaviours.getX( ) && 
                                                panel.getMousePosition( ).x < btnAllowedBehaviours.getX( ) + btnAllowedBehaviours.getWidth( ) &&
                                                panel.getMousePosition( ).y > btnAllowedBehaviours.getY( ) && 
                                                panel.getMousePosition( ).y < btnAllowedBehaviours.getY( ) + btnAllowedBehaviours.getHeight( ) ) );
                                        }
                                        else
                                        {
                                            btnAllowedBehaviours.setEnabled( true );
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled( PopupMenuEvent e )
                                    {
                                    }
                                } );
                                behaviourPopup.show( btnAllowedBehaviours, 0, btnAllowedBehaviours.getHeight( ) );
                                btnAllowedBehaviours.requestFocusInWindow( );
                            }
                        } );
                        
                        final JButton btnSettings = new JButton( languageBundle.getString( "Settings" ) );
                        btnSettings.addMouseListener( new MouseListener( )
                        {
                            @Override
                            public void mouseClicked( MouseEvent e )
                            {
                            }

                            @Override
                            public void mousePressed( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseReleased( MouseEvent e )
                            {
                                btnSettings.setEnabled( true );
                            }

                            @Override
                            public void mouseEntered( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseExited( MouseEvent e )
                            {
                            }
                        } );
                        btnSettings.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                JMenuItem chooseShimejiMenu = new JMenuItem( languageBundle.getString( "ChooseShimeji" ) );
                                chooseShimejiMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );

                                        // Get the image sets to use
                                        ArrayList<String> temporaryImageSet = new ArrayList<String>( );
                                        temporaryImageSet = new ImageSetChooser( frame, true ).display( );
                                        if( temporaryImageSet != null )
                                        {
                                            imageSets = temporaryImageSet;
                                        }

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );

                                // "Interactive Windows" menu item
                                JMenuItem interactiveMenu = new JMenuItem( languageBundle.getString( "ChooseInteractiveWindows" ) );
                                interactiveMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        new WindowsInteractiveWindowForm( frame, true ).display( );
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                    }
                                } );

                                // "Scaling" menu
                                JMenu scalingMenu = new JMenu( languageBundle.getString( "Scaling" ) );
                                final JCheckBoxMenuItem scaling1x = new JCheckBoxMenuItem( "1x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 1 );
                                scaling1x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "1" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                final JCheckBoxMenuItem scaling2x = new JCheckBoxMenuItem( "2x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 2 );
                                scaling2x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "2" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                final JCheckBoxMenuItem scaling3x = new JCheckBoxMenuItem( "3x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 3 );
                                scaling3x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "3" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                final JCheckBoxMenuItem scaling4x = new JCheckBoxMenuItem( "4x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 4 );
                                scaling4x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "4" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                final JCheckBoxMenuItem scaling6x = new JCheckBoxMenuItem( "6x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 6 );
                                scaling6x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "6" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                final JCheckBoxMenuItem scaling8x = new JCheckBoxMenuItem( "8x", Integer.parseInt( properties.getProperty( "Scaling", "1" ) ) == 8 );
                                scaling8x.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        properties.setProperty( "Scaling", "8" );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                        
                                        // need to reload the shimeji as the images have rescaled
                                        boolean isExit = getManager( ).isExitOnLastRemoved( );
                                        getManager( ).setExitOnLastRemoved( false );
                                        getManager( ).disposeAll( );
                                        
                                        // Wipe all loaded data
                                        ImagePairs.imagepairs.clear( );
                                        configurations.clear( );

                                        // Load settings
                                        for( String imageSet : imageSets )
                                        {
                                            loadConfiguration( imageSet );
                                        }

                                        // Create the first mascot
                                        for( String imageSet : imageSets )
                                        {
                                            createMascot( imageSet );
                                        }

                                        Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                    }
                                } );
                                scalingMenu.add( scaling1x );
                                scalingMenu.add( scaling2x );
                                scalingMenu.add( scaling3x );
                                scalingMenu.add( scaling4x );
                                scalingMenu.add( scaling6x );
                                scalingMenu.add( scaling8x );
                                
                                JPopupMenu settingsPopup = new JPopupMenu( );
                                settingsPopup.add( chooseShimejiMenu );
                                settingsPopup.add( interactiveMenu );
                                settingsPopup.add( new JSeparator( ) );
                                settingsPopup.add( scalingMenu );
                                settingsPopup.addPopupMenuListener( new PopupMenuListener( )
                                {
                                    @Override
                                    public void popupMenuWillBecomeVisible( PopupMenuEvent e )
                                    {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
                                    {
                                        if( panel.getMousePosition( ) != null )
                                        {
                                            btnSettings.setEnabled( !( panel.getMousePosition( ).x > btnSettings.getX( ) && 
                                                panel.getMousePosition( ).x < btnSettings.getX( ) + btnSettings.getWidth( ) &&
                                                panel.getMousePosition( ).y > btnSettings.getY( ) && 
                                                panel.getMousePosition( ).y < btnSettings.getY( ) + btnSettings.getHeight( ) ) );
                                        }
                                        else
                                        {
                                            btnSettings.setEnabled( true );
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled( PopupMenuEvent e )
                                    {
                                    }
                                } );
                                settingsPopup.show( btnSettings, 0, btnSettings.getHeight( ) );
                                btnSettings.requestFocusInWindow( );
                            }
                        } );
                        
                        //"i once decided it was time to write a long sentence but I wasn't sure how to start it so I just started writing about how I was going to write a long sentence but I wasn't sure how to start it so I just started writing"
                        final JButton btnLanguage = new JButton( languageBundle.getString( "Language" ) );
                        btnLanguage.addMouseListener( new MouseListener( )
                        {
                            @Override
                            public void mouseClicked( MouseEvent e )
                            {
                            }

                            @Override
                            public void mousePressed( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseReleased( MouseEvent e )
                            {
                                btnLanguage.setEnabled( true );
                            }

                            @Override
                            public void mouseEntered( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseExited( MouseEvent e )
                            {
                            }
                        } );
                        btnLanguage.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                // English menu item
                                final JMenuItem englishMenu = new JMenuItem( "English" );
                                englishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "en-GB" ) )
                                        {
                                            properties.setProperty( "Language", "en-GB" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Catalan menu item
                                final JMenuItem catalanMenu = new JMenuItem( "Catal\u00E0" );
                                catalanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "ca-ES" ) )
                                        {
                                            properties.setProperty( "Language", "ca-ES" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // German menu item
                                final JMenuItem germanMenu = new JMenuItem( "Deutsch" );
                                germanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "de-DE" ) )
                                        {
                                            properties.setProperty( "Language", "de-DE" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Spanish menu item
                                final JMenuItem spanishMenu = new JMenuItem( "Espa\u00F1ol" );
                                spanishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "es-ES" ) )
                                        {
                                            properties.setProperty( "Language", "es-ES" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // French menu item
                                final JMenuItem frenchMenu = new JMenuItem( "Fran\u00E7ais" );
                                frenchMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "fr-FR" ) )
                                        {
                                            properties.setProperty( "Language", "fr-FR" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Croatian menu item
                                final JMenuItem croatianMenu = new JMenuItem( "Hrvatski" );
                                croatianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "hr-HR" ) )
                                        {
                                            properties.setProperty( "Language", "hr-HR" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Italian menu item
                                final JMenuItem italianMenu = new JMenuItem( "Italiano" );
                                italianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "it-IT" ) )
                                        {
                                            properties.setProperty( "Language", "it-IT" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Dutch menu item
                                final JMenuItem dutchMenu = new JMenuItem( "Nederlands" );
                                dutchMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "nl-NL" ) )
                                        {
                                            properties.setProperty( "Language", "nl-NL" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Polish menu item
                                final JMenuItem polishMenu = new JMenuItem( "Polski" );
                                polishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "pl-PL" ) )
                                        {
                                            properties.setProperty( "Language", "pl-PL" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Brazilian Portuguese menu item
                                final JMenuItem brazilianPortugueseMenu = new JMenuItem( "Portugu\u00eas Brasileiro" );
                                brazilianPortugueseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "pt-BR" ) )
                                        {
                                            properties.setProperty( "Language", "pt-BR" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Portuguese menu item
                                final JMenuItem portugueseMenu = new JMenuItem( "Portugu\u00eas" );
                                portugueseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "pt-PT" ) )
                                        {
                                            properties.setProperty( "Language", "pt-PT" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Russian menu item
                                final JMenuItem russianMenu = new JMenuItem( "\u0440\u0443\u0301\u0441\u0441\u043a\u0438\u0439 \u044f\u0437\u044b\u0301\u043a" );
                                russianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "ru-RU" ) )
                                        {
                                            properties.setProperty( "Language", "ru-RU" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Romanian menu item
                                final JMenuItem romanianMenu = new JMenuItem( "Rom\u00e2n\u0103" );
                                romanianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "ro-RO" ) )
                                        {
                                            properties.setProperty( "Language", "ro-RO" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Srpski menu item
                                final JMenuItem serbianMenu = new JMenuItem( "Srpski" );
                                serbianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "sr-RS" ) )
                                        {
                                            properties.setProperty( "Language", "sr-RS" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Finnish menu item
                                final JMenuItem finnishMenu = new JMenuItem( "Suomi" );
                                finnishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "fi-FI" ) )
                                        {
                                            properties.setProperty( "Language", "fi-FI" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Vietnamese menu item
                                final JMenuItem vietnameseMenu = new JMenuItem( "ti\u1ebfng Vi\u1ec7t" );
                                vietnameseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "vi-VN" ) )
                                        {
                                            properties.setProperty( "Language", "vi-VN" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Chinese menu item
                                final JMenuItem chineseMenu = new JMenuItem( "\u7b80\u4f53\u4e2d\u6587" );
                                chineseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "zh-CN" ) )
                                        {
                                            properties.setProperty( "Language", "zh-CN" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );

                                // Korean menu item
                                final JMenuItem koreanMenu = new JMenuItem( "\ud55c\uad6d\uc5b4" );
                                koreanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        if( !properties.getProperty( "Language", "en-GB" ).equals( "ko-KR" ) )
                                        {
                                            properties.setProperty( "Language", "ko-KR" );
                                            refreshLanguage( );
                                        }
                                        NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                                        try
                                        {
                                            FileOutputStream output = new FileOutputStream( "./conf/settings.properties" );
                                            try
                                            {
                                                properties.store( output, "Shimeji-ee Configuration Options" );
                                            }
                                            finally
                                            {
                                                output.close( );
                                            }
                                        }
                                        catch( Exception unimportant )
                                        {
                                        }
                                    }
                                } );
                                
                                JPopupMenu languagePopup = new JPopupMenu( );
                                languagePopup.add( englishMenu );
                                languagePopup.addSeparator( );
                                languagePopup.add( catalanMenu );
                                languagePopup.add( germanMenu );
                                languagePopup.add( spanishMenu );
                                languagePopup.add( frenchMenu );
                                languagePopup.add( croatianMenu );
                                languagePopup.add( italianMenu );
                                languagePopup.add( dutchMenu );
                                languagePopup.add( polishMenu );
                                languagePopup.add( portugueseMenu );
                                languagePopup.add( brazilianPortugueseMenu );
                                languagePopup.add( russianMenu );
                                languagePopup.add( romanianMenu );
                                languagePopup.add( serbianMenu );
                                languagePopup.add( finnishMenu );
                                languagePopup.add( vietnameseMenu );
                                languagePopup.add( chineseMenu );
                                languagePopup.add( koreanMenu );
                                languagePopup.addPopupMenuListener( new PopupMenuListener( )
                                {
                                    @Override
                                    public void popupMenuWillBecomeVisible( PopupMenuEvent e )
                                    {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
                                    {
                                        if( panel.getMousePosition( ) != null )
                                        {
                                            btnLanguage.setEnabled( !( panel.getMousePosition( ).x > btnLanguage.getX( ) && 
                                                panel.getMousePosition( ).x < btnLanguage.getX( ) + btnLanguage.getWidth( ) &&
                                                panel.getMousePosition( ).y > btnLanguage.getY( ) && 
                                                panel.getMousePosition( ).y < btnLanguage.getY( ) + btnLanguage.getHeight( ) ) );
                                        }
                                        else
                                        {
                                            btnLanguage.setEnabled( true );
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled( PopupMenuEvent e )
                                    {
                                    }
                                } );
                                languagePopup.show( btnLanguage, 0, btnLanguage.getHeight( ) );
                                btnLanguage.requestFocusInWindow( );
                            }
                        } );
                        
                        JButton btnDismissAll = new JButton( languageBundle.getString( "DismissAll" ) );
                        btnDismissAll.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                exit( );
                            }
                        } );

                        // layout
                        float scaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;
                        panel.setLayout( new java.awt.GridBagLayout( ) );
                        GridBagConstraints gridBag = new GridBagConstraints( );
                        gridBag.fill = GridBagConstraints.HORIZONTAL;
                        gridBag.gridx = 0;
                        gridBag.gridy = 0;
                        panel.add( btnCallShimeji, gridBag );
                        gridBag.insets = new Insets( (int)( 5 * scaling ), 0, 0, 0 );
                        gridBag.gridy++;
                        panel.add( btnFollowCursor, gridBag );
                        gridBag.gridy++;
                        panel.add( btnReduceToOne, gridBag );
                        gridBag.gridy++;
                        panel.add( btnRestoreWindows, gridBag );
                        gridBag.gridy++;
                        panel.add( new JSeparator( ), gridBag );
                        gridBag.gridy++;
                        panel.add( btnAllowedBehaviours, gridBag );
                        gridBag.gridy++;
                        panel.add( btnSettings, gridBag );
                        gridBag.gridy++;
                        panel.add( btnLanguage, gridBag );
                        gridBag.gridy++;
                        panel.add( new JSeparator( ), gridBag );
                        gridBag.gridy++;
                        panel.add( btnDismissAll, gridBag );

                        try
                        {
                            form.setIconImage( ImageIO.read( Main.class.getResource( "/icon.png" ) ) );
                        }
                        catch( IOException ex )
                        {
                        }
                        
                        form.setTitle( languageBundle.getString( "ShimejiEE" ) );
                        form.setDefaultCloseOperation( javax.swing.WindowConstants.DISPOSE_ON_CLOSE );
                        form.setAlwaysOnTop( true );
                        
                        // set the form dimensions
                        java.awt.FontMetrics metrics = btnCallShimeji.getFontMetrics( btnCallShimeji.getFont( ) );
                        int width = metrics.stringWidth( btnCallShimeji.getText( ) );
                        width = Math.max( metrics.stringWidth( btnFollowCursor.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnReduceToOne.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnRestoreWindows.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnAllowedBehaviours.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnSettings.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnLanguage.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnDismissAll.getText( ) ), width );
                        panel.setPreferredSize( new Dimension( width + 64, 
                                (int)( 24 * scaling ) + // 12 padding on top and bottom
                                (int)( 45 * scaling ) + // 9 insets of 5 height normally
                                8 * metrics.getHeight( ) + // 8 button faces
                                84 ) );
                        form.pack( );
                        
                        // setting location of the form
                        form.setLocation( event.getPoint( ).x - form.getWidth( ), event.getPoint( ).y - form.getHeight( ) );
                        
                        // make sure that it is on the screen if people are using exotic taskbar locations
                        Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment( ).getMaximumWindowBounds( );
                        if( form.getX( ) < screen.getX( ) )
                        {
                            form.setLocation( event.getPoint( ).x, form.getY( ) );
                        }
                        if( form.getY( ) < screen.getY( ) )
                        {
                            form.setLocation( form.getX( ), event.getPoint( ).y );
                        }
                        form.setVisible( true );
                        form.setMinimumSize( form.getSize( ) );
                    }
                    else if( event.getButton( ) == MouseEvent.BUTTON1 )
                    {
                        createMascot( );
                    }
                }

                @Override
                public void mouseEntered( MouseEvent e )
                {
                }

                @Override
                public void mouseExited( MouseEvent e )
                {
                }
            } );

            // Show tray icon
            SystemTray.getSystemTray( ).add( icon );
        }
        catch( final IOException e )
        {
            log.log( Level.SEVERE, "Failed to create tray icon", e );
            Main.showError( languageBundle.getString( "FailedDisplaySystemTrayErrorMessage" ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            exit( );
        }
        catch( final AWTException e )
        {
            log.log( Level.SEVERE, "Failed to create tray icon", e );
            Main.showError( languageBundle.getString( "FailedDisplaySystemTrayErrorMessage" ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            exit( );
        }
    }

    // Randomly creates a mascot
    public void createMascot( )
    {
        int length = imageSets.size();
        int random = ( int ) ( length * Math.random( ) );
        createMascot( imageSets.get( random ) );
    }

    /**
     * Create a mascot
     */
    public void createMascot( String imageSet )
    {
        log.log( Level.INFO, "create a mascot" );

        // Create one mascot
        final Mascot mascot = new Mascot( imageSet );

        // Create it outside the bounds of the screen
        mascot.setAnchor( new Point( -4000, -4000 ) );

        // Randomize the initial orientation
        mascot.setLookRight( Math.random( ) < 0.5 );

        try
        {
            mascot.setBehavior( getConfiguration( imageSet ).buildBehavior( null, mascot ) );
            this.getManager().add( mascot );
        }
        catch( final BehaviorInstantiationException e )
        {
            log.log( Level.SEVERE, "Failed to initialize the first action", e );
            Main.showError( languageBundle.getString( "FailedInitialiseFirstActionErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            mascot.dispose();
        }
        catch( final CantBeAliveException e )
        {
            log.log( Level.SEVERE, "Fatal Error", e );
            Main.showError( languageBundle.getString( "FailedInitialiseFirstActionErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            mascot.dispose();
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, imageSet + " fatal error, can not be started.", e );
            Main.showError( languageBundle.getString( "CouldNotCreateShimejiErrorMessage" ) + imageSet + ".\n" + e.getMessage( ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            mascot.dispose();
        }
    }
    
    private void refreshLanguage( )
    {
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl( false );
        languageBundle = ResourceBundle.getBundle( "language", Locale.forLanguageTag( properties.getProperty( "Language", "en-GB" ) ), utf8Control );

        boolean isExit = getManager( ).isExitOnLastRemoved( );
        getManager( ).setExitOnLastRemoved( false );
        getManager( ).disposeAll( );

        // Load settings
        for( String imageSet : imageSets )
        {
            loadConfiguration( imageSet );
        }

        // Create the first mascot
        for( String imageSet : imageSets )
        {
            createMascot( imageSet );
        }

        getManager( ).setExitOnLastRemoved( isExit );
    }

    public Configuration getConfiguration( String imageSet )
    {
        return configurations.get( imageSet );
    }

    private Manager getManager( )
    {
        return this.manager;
    }
        
    public Platform getPlatform( )
    {
        return platform;
    }

    public Properties getProperties( )
    {
        return properties;
    }

    public ResourceBundle getLanguageBundle( )
    {
        return languageBundle;
    }

    public void exit()
    {
        this.getManager().disposeAll();
        this.getManager().stop();


        System.exit( 0 );
    }
}
