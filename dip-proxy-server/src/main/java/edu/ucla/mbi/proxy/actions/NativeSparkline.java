package edu.ucla.mbi.proxy.actions;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * NativeSparkline Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.*;

import java.util.*;

import edu.ucla.mbi.util.Sparkline;

import edu.ucla.mbi.server.WSContext;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;

public class NativeSparkline extends ActionSupport {
    
    private String provider = null;
    private String service = null;
    private String range = null;
    private String mode = null;

    private InputStream imageStream = null;
    
    private WSContext wsContext = null;

    private Sparkline sparkline = null;

    public void setPrv( String provider ){
        this.provider = provider;
    }

    public void setSrv( String service ){
        this.service = service;
    }

    public void setRange( String range ){
        this.range = range;
    }
     
    public void setMode( String mode ){
        this.mode = mode;
    }

    public void setWsContext( WSContext wsContext ){
        this.wsContext = wsContext;
    }
   
    public void setSparkline( Sparkline sparkline ) {
        this.sparkline = sparkline;
    }
    
    public InputStream getImageStream(){
        return imageStream;
    }

   
    //---------------------------------------------------------------------
    
    public String execute() throws Exception {
        
        Log log = LogFactory.getLog( NativeSparkline.class );

        NativeAuditDAO nad = DipProxyDAO.getNativeAuditDAO();
        
        int count = 12*24; // 1 day

        if ( range != null ) {

            if ( range.equals( "day" ) ) {
                count = 12*24;
            }

            if ( range.equals( "week" ) ) {
                count = 12*24*7;
            }
            
            if ( range.equals( "month" ) ) {
                count = 12*24*7*30;
            }
        }
        
        java.util.List<long[]> series = 
            nad.findLastList( provider, service, count );

        /*
        java.util.List<Long> trace = new ArrayList();
        
        for( Iterator<long[]> ii = series.iterator(); 
             ii.hasNext(); ) {
            long[] i = ii.next();
            trace.add( i[1] );           
        }
        */

        BufferedImage bufferedImage = 
        //    sparkline.build( trace, mode );
                sparkline.build( series, mode );
        try {
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();            
            ImageIO.write( bufferedImage, "png", os );

            byte[] byteImage = os.toByteArray();
            imageStream = new ByteArrayInputStream( byteImage ); 
            
        } catch (IOException e) {

        }

        setMessage( getText( MESSAGE ) );
        return "png";

        
        /*
                                               
        int width = 240;
        int height = 20;
        
        // Create a buffered image in which to draw
        BufferedImage bufferedImage = 
            new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    
        // Create a graphics contents on the buffered image

        Graphics2D g2 = bufferedImage.createGraphics();
        
        RenderingHints renderHints =
            new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.setRenderingHints(renderHints);
        g2.setColor(Color.white);
        g2.fillRect(0,0,width,height);

        GeneralPath p = new GeneralPath(GeneralPath.WIND_NON_ZERO); 
        p.moveTo( 0, 1); 


        // go over list
        
        int lcount = series.size();

        int x = 0;
        double y =0;
        Color line = new Color(0,   128,   0 );
        Color fill = new Color(64,  255,  64 ); 
        
        for ( Iterator<long[]> ii = series.iterator(); 
              ii.hasNext(); ) {
            
            long[] i = ii.next();

            if ( mode != null && mode.equals( "log" ) ) {
                y =  Math.log(4.0*i[1] ); 
            } else {
                y =  Math.exp( 0.0001*i[1] ); 
            }
            
            if (y > 20 ){
                y = 20.0;
            }

            if ( x==0 ){
                p.moveTo( width*x/lcount, y);
                if ( i[1]>1000 ) {
                    line = new Color( 64,  64,  0 );
                    fill = new Color(153, 255,  0 );
                }
                if ( i[1]>10000 ) {
                    line = new Color(128,   0,   0 );
                    fill = new Color(255,   0,   0 );
                }

            } else {
                p.lineTo( width*x/lcount, y);
            }
            x++;
        }

        p.lineTo( width, y);
        
        GeneralPath p2= (GeneralPath) p.clone();
  
        p2.lineTo(240, 30);
        p2.lineTo(0, 30);
        p2.closePath(); 

        g2.setColor( fill );
        g2.fill(p2);
        
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor( line );
        g2.draw(p);
        
        g2.dispose();
        */
        
    }

    /**
     * Provide default value for Message property.
     */
    public static final String MESSAGE = "foo.message";

    /**
     * Field for Message property.
     */
    private String message;

    /**
     * Return Message property.
     *
     * @return Message property
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set Message property.
     *
     * @param message Text to display on HelloWorld page.
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
