package edu.ucla.mbi.util;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * Sparkline Icon generator:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.*;

import java.util.*;

public class Sparkline {
    
    private double xRange = 240;
    private double yRange =  30;
    
    private String mode = "exp";

    private int width = 240;
    private int height = 20;

    private java.util.List<Integer> faultStatusList;
 
    public void setXRange( double range ) {
        this.xRange = range;
    }
    public void setYRange( double range ) {
        this.yRange = range;
    } 

    public void setWidth( int width ) {
        this.width = width;
    }

    public void setHeight( int height ) {
        this.height = height;
    }

    public void setMode( String mode ) {
        this.mode = mode;
    }

    public void setFaultStatusList ( java.util.List<Integer> statusList ) {
        this.faultStatusList = statusList;
    }

    //---------------------------------------------------------------------
    
    public BufferedImage build( java.util.List<Long> trace, String mode,
                                int lastAuditStatus ) { 
   
        Log log = LogFactory.getLog( Sparkline.class );
        
        BufferedImage bufferedImage = 
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g2 = bufferedImage.createGraphics();
        
        RenderingHints renderHints =
            new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY); 
        g2.setRenderingHints(renderHints);
        
        // clear background
        //-----------------
        
        g2.setColor(Color.white);
        g2.fillRect(0,0,width,height);

        // build trace path
        //-----------------

        GeneralPath p = new GeneralPath(GeneralPath.WIND_NON_ZERO); 
        
        int lcount = trace.size();
        double xMax = lcount+1;
        
        // go over list

        int x = 0;
        double y =0;
        Color line = new Color(0,  128,   0 );
        Color fill = new Color(64, 255,  64 ); 
        
        double yLogScl= height / Math.log( yRange + 1);
        double yExpScl= Math.log( height ) / yRange;
        double xScl= width/xMax;
        
        for ( Iterator<Long> ii = trace.iterator(); 
              ii.hasNext(); ) {
            
            long i = ii.next().longValue();
            if ( mode != null && mode.equals( "log" ) ) {
                y =  1+yLogScl * Math.log( i + 1); 
            } else {
                y =  1+Math.exp( yExpScl*i ); 
            }
            
            if (y > height ){
                y = height;
            }

            if ( x==0 ) {
                p.moveTo( xScl * x, y );
                if ( 30.0 * i > yRange ){
                    line = new Color(128,  64,  0 );
                    fill = new Color(255, 204,  0 );
                }

                if ( 3.0 * i > yRange 
                        || faultStatusList.contains( lastAuditStatus ) ) 
                { 
                    line = new Color(128,   0,   0 );
                    fill = new Color(255,   0,   0 );
                }
            } else {
                p.lineTo( xScl * x, y );
            }

            x++;
        }

        p.lineTo( width, y );
        
        GeneralPath p2= (GeneralPath) p.clone();
  
        p2.lineTo( width, height);
        p2.lineTo( 0, height );
        p2.closePath(); 

        g2.setColor( fill );
        g2.fill( p2 );
        
        g2.setStroke(new BasicStroke( 1.0f ) );
        g2.setColor( line );
        g2.draw(p);
        
        g2.dispose();

        return bufferedImage;
    }
}
