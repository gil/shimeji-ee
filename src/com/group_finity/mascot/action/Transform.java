package com.group_finity.mascot.action;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Original Author: Yuki Yamada of Group Finity
 * (http://www.group-finity.com/Shimeji/) Currently developed by Shimeji-ee
 * Group.
 */
public class Transform extends Animate
{
    private static final Logger log = Logger.getLogger( Transform.class.getName( ) );
    
    public static final String PARAMETER_TRANSFORMBEHAVIOR = "TransformBehavior";

    private static final String DEFAULT_TRANSFORMBEHAVIOR = "";

    public static final String PARAMETER_TRANSFORMMASCOT = "TransformMascot";

    private static final String DEFAULT_TRANSFORMMASCOT = "";

    public Transform( final List<Animation> animations, final VariableMap params )
    {
        super( animations, params );
    }

    @Override
    protected void tick( ) throws LostGroundException, VariableException
    {
        super.tick( );

        if( getTime( ) == getAnimation( ).getDuration( ) - 1 )
        {
            transform( );
        }
    }

    private void transform( ) throws VariableException
    {
        String childType = Main.getInstance( ).getConfiguration( getTransformMascot( ) ) != null ? getTransformMascot( ) : getMascot( ).getImageSet( );
        
        try
        {
            getMascot( ).setImageSet( childType );
            getMascot( ).setBehavior( Main.getInstance( ).getConfiguration( childType ).buildBehavior( getTransformBehavior( ) ) );
        }
        catch( final BehaviorInstantiationException e )
        {
            log.log( Level.SEVERE, "Fatal Exception", e );
            Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedCreateNewShimejiErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + Main.getInstance( ).getLanguageBundle( ).getString( "SeeLogForDetails" ) );
        }
        catch( final CantBeAliveException e )
        {
            log.log( Level.SEVERE, "Fatal Exception", e );
            Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedCreateNewShimejiErrorMessage" ) + "\n" + e.getMessage( ) + "\n" + Main.getInstance( ).getLanguageBundle( ).getString( "SeeLogForDetails" ) );
        }
    }

    private String getTransformBehavior( ) throws VariableException
    {
        return eval( PARAMETER_TRANSFORMBEHAVIOR, String.class, DEFAULT_TRANSFORMBEHAVIOR );
    }

    private String getTransformMascot( ) throws VariableException
    {
        return eval( PARAMETER_TRANSFORMMASCOT, String.class, DEFAULT_TRANSFORMMASCOT );
    }
}
