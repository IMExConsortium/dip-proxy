YAHOO.namespace("mbi");

YAHOO.mbi.prefmgr = {
    
    preferences : '',
    viewUrl:'',
    updateUrl:'',
    defsetUrl:'',
    toppref:'config', 

    /*************************************************************************
     * Takes an object and recursively travels down all the nodes.
     * function is applied when it reaches an object with a option-def property
     *************************************************************************/
    traverse:  function( object, func ){

        var html = '';
           
        for (var i in object) {                   
            if(i =="option-def"){
                html += "\n<div class='cfg-block-list'>\n";
                for( var j = 0; j < object.options.length; j++ ){
                    
                    var strong = false;
                    
                    if( object["option-def"][object.options[j]]["legend"] !== undefined){
                        html += "<div class='cfg-block-legend'>";    
                        html += "\n<fieldset>\n";
                        html += "<legend>" + object["option-def"][object.options[j]]["legend"] +"</legend>";
                        strong = true;
                    }else{
                        html += "<div class='cfg-block-no-legend'>";   
                    }
                    
                    html += func
                        .apply( this, [ j, 
                                        object["option-def"][object.options[j] ], 
                                        object["options"], 
                                        object["option-def"][object.options[j]]["opp"],
                                        strong
                                      ] ); 
                    
                    //going to step down in the object tree!!
                    
                    html += this
                        .traverse( object[ "option-def"][ object.options[j] ], 
                                   func );
                    
                    if( object["option-def"][object.options[j]]["legend"] !== undefined){
                        html += "\n</fieldset>\n";
                    }
                    html += "</div>\n";
                    
                }
                
                html+="</div>\n";                    
            }
        }
        return html;
    },
    /*************************************************************************
     * Initializes the prefmgr page with a logged in users preferences  
     * also creates the structure and displays the form element on the page
     *************************************************************************/ 
    init: function( init ){
        var prefmgr = YAHOO.mbi.prefmgr;
        prefmgr.viewUrl = init.viewUrl;
        prefmgr.updateUrl = init.updateUrl;
        prefmgr.defsetUrl = init.defsetUrl;
        if( init.defsetUrl !== undefined){
            //prefmgr.toppref = init.pref;
        }
        var Success = function( response ){

            var process = function( key, value, options, opp, strong ){
                
                var upm = YAHOO.mbi.prefmgr;
                
                if( typeof value.value != "undefined"){
                    
                    var keyClass = "cfg-key"; 
                    
                    if( strong ){
                        keyClass = "cfg-key-strong";
                    }
                    return "<div class='cfg-key-val'>" 
                        + "<div class='" + keyClass + "'>" + value.label + "</div>" 
                        + "<div class='cfg-val' id='opp." + opp + "'>" 
                        + upm.htmlBoolRadio( options[key], opp, value.value )
                        + "</div>"
                        + "</div>";
                }
                return "";
            };
            
            console.log("sucess");
            
           
            prefmgr.preferences 
                = YAHOO.lang.JSON.parse( response.responseText );

           
            prefmgr.preferences 
                = YAHOO.lang.JSON.parse( prefmgr.preferences[YAHOO.mbi.prefmgr.toppref] );
            console.log( prefmgr.preferences );     
            
            var form = document.getElementById(init["formid"]);
            var html = '';

            html += prefmgr.traverse( prefmgr.preferences, process );
            console.log ( html );
            form.innerHTML = html + form.innerHTML ;                             
        };
        var Fail = function ( o ) {
            console.log( "AJAX Error update failed: id=" + o.argument.id ); 
        };
        var callback = { cache:false, timeout: 5000, 
                         success: Success,
                         failure: Fail
                         }; 
        
        try{
            YAHOO.util.Connect
            .asyncRequest( 'GET', prefmgr.viewUrl, callback );        
        } catch (x) {
            console.log("AJAX Error:"+x);
        }
    },
    
    updateForm: function(){
        var upm = YAHOO.mbi.prefmgr;
        console.log("In function update");
        
        var html='';
        var process = function (key, value, options, opp, strong  ){   
            if( typeof value.value != "undefined" ){                
                var valDiv = YAHOO.util.Dom.get( "opp." + opp );
                if( valDiv !== undefined ){
                    valDiv.innerHTML 
                        = upm.htmlBoolRadio( key, opp, value.value );
                }
            }
        };
        try{
            upm.traverse( upm.preferences, process );            
        } catch (x) {
            console.log("updateForm: Traverse Error:" + x );
        }
    },
    
    htmlBoolRadio: function( optName, optOpp, optValue ){
        
        if( optValue ==='true'){
            var checkboxT = '<input type="radio" id="' 
                + optName +'True" name="opp.' + optOpp + '"' 
                +' value="true" checked="checked" ><strong>True</strong></input>';
            var checkboxF = '<input type="radio" id="' 
                + optName +'False" name="opp.' + optOpp + '"' 
                +' value="false">False</input>';
        }else{
            var checkboxT = '<input type="radio" id="' 
                + optName +'True" name="opp.' + optOpp + '"' 
                + ' value="true">True</input>';
            var checkboxF = '<input type="radio" id="' 
                + optName +'False" name="opp.' + optOpp +'"' 
                +' value="false" checked="checked" ><strong>False</strong></input>';
        }
        var html = "<div class='cfg-val'>" + checkboxT + " " +  checkboxF +"</div>";
        return html;
    },

    sendUpdatedPrefs: function( o  ){
        var prefmgr = YAHOO.mbi.prefmgr;
        var Success = function ( o ) {
            console.log( 'updated' ); 
        };
        var Fail = function ( o ) {
            console.log( "AJAX Error update failed: id=" + o.argument.id ); 
        };
        var callback = { cache:false, timeout: 5000, 
                         success: Success,
                         failure: Fail
                         };    
        try{    
            YAHOO.util.Connect
                .asyncRequest( 'POST', 
                               prefmgr.updateUrl, 
                               callback,  
                               YAHOO.lang.JSON.stringify( prefmgr.preferences));        
        } catch (x) {
            console.log("AJAX Error:"+x);
        }
    },

    submit: function ( formid ){
        var prefmgr = YAHOO.mbi.prefmgr;

        var Success = function ( o ) {
            console.log( 'updated' );
            // update preferences fields
            
            var upm = YAHOO.mbi.prefmgr;
   
            upm.preferences 
                = YAHOO.lang.JSON.parse( o.responseText );
            upm.preferences 
                = YAHOO.lang.JSON.parse( upm.preferences.preferences );

            upm.updateForm();   
            
        };
        
        var Fail = function ( o ) {
            console.log( "AJAX Error update failed: id=" + o.argument.id ); 
        };
        var callback = { cache:false, timeout: 5000, 
                         success: Success,
                         failure: Fail
                       };             

        alert("submit to:" + prefmgr.updateUrl );
        try{
            var formObj = document.getElementById( formid );
            YAHOO.util.Connect.setForm(formObj);
            alert("formObj");            

            var cObj = YAHOO.util.Connect
                .asyncRequest( 'POST', 
                               prefmgr.updateUrl, 
                               callback );            
        } catch (x) {
            console.log("AJAX Error:"+x);
        }   
    },
    
    defset: function( o ){
        var prefmgr = YAHOO.mbi.prefmgr;
        var callback = { cache:false, timeout: 5000, 
                         success: Success,
                         failure: Fail
                         }; 

        var Success = function ( o ) {
            console.log( 'defset' ); 

            var upm = YAHOO.mbi.prefmgr;
   
            upm.preferences 
                = YAHOO.lang.JSON.parse( o.responseText );
            upm.preferences 
                = YAHOO.lang.JSON.parse( upm.preferences.preferences );
            
            upm.updateForm();   
        };
        
        var Fail = function ( o ) {
            console.log( "AJAX Error defset failed: id=" + o.argument.id ); 
        };
        
        try{
            YAHOO.util
                .Connect.asyncRequest( 'GET', prefmgr.defsetUrl, 
                                       callback );        
        } catch (x) {
            console.log("AJAX Error:"+x);
        }
    }
};
