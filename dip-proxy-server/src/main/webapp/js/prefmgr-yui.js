YAHOO.namespace("mbi");

YAHOO.mbi.prefmgr = {
    
    preferences : '',
    viewUrl:'',
    updateUrl:'',
    defsetUrl:'',
    menuButton:'',
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
                        + upm.htmlManager( options[key], opp, value  )
                        + "</div>"
                        + "</div>";
                }
                return "";
            };
            
            console.log("Success");
            
            prefmgr.preferences 
                = YAHOO.lang.JSON.parse( response.responseText );
            prefmgr.preferences 
                = YAHOO.lang.JSON.parse( prefmgr.preferences[prefmgr.toppref] );
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
                        = upm.htmlManager( options[key], opp, value);
                }
            }
        };
        try{
            upm.traverse( upm.preferences, process );            
        } catch (x) {
            console.log("updateForm: Traverse Error:" + x );
        }
    },

    htmlManager: function( name, opp, value ){
        var upm = YAHOO.mbi.prefmgr;
        if( typeof value.type == "undefined"){
            return upm.htmlBoolRadio( name, opp, value.value  );
        }
        switch( value.type.toLowerCase() )
        {
        case "boolean" :
           return upm.htmlBoolRadio( name, opp, value.value  );
           break;
        case "string" :
            return upm.htmlTextField( name, opp, value  );
            break;
        case "menu" :
            return upm.htmlMenu( name, opp, value  );
            break;
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
    htmlTextField: function( name, opp, value ){
        //return  name + ':' +
        return ' <input type="text" id="'
                + name + '" maxlength="' + value.length  + '" '  
                + 'name="opp.' + opp + '" size="'+ value.length + '" '
                + 'value="' + value.value + '">';
    },
    
    onMenuItemClick : function ( menuChange ) 
    {
        var menuItem = menuChange.newValue;
        var newText = menuItem.cfg.getProperty("text");
        var input = document.getElementById(this.my.inputId);
        this.set("label", newText);
        input.value = menuItem.value;
    },
    
    htmlMenu: function( name, opp, value ){
        
        var menuItems = value["value-list"];
        var div = document.createElement('div');
        div.setAttribute("id", name + "MenuButton");
        
        var input = document.createElement('input')
        input.setAttribute("name", "opp." + opp);
        input.setAttribute("type", "hidden" );
        input.setAttribute("value", value.value );
        input.setAttribute("id",  name + "Input" );
        div.appendChild(input);
        
        var menuList = value["value-list"];
        for(var i = 0; i < menuList.length; i++)
        {
            if(menuList[i].value == value.value )
            {
                var menuLabel = menuList[i].text;
                break;
            }
        }
        var menuButton = new YAHOO.widget.Button({ 
                        id: opp, 
                        name: name,
                        label: "<em class=\"yui-button-label\">" + menuLabel + "</em>",
                        type: "menu",  
                        menu: value["value-list"], 
                        container: name + "MenuButton" });
                        
        menuButton.on( "selectedMenuItemChange", 
                               YAHOO.mbi.prefmgr.onMenuItemClick );
        menuButton.my = {"inputId" : name + "Input"};
        return div.outerHTML;
        
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
        try{
            var formObj = document.getElementById( formid );
            YAHOO.util.Connect.setForm(formObj);
            
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
