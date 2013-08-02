<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>



<form id="dht-config-form" name="dht-config-form" onsubmit="return false;" action="" method="post">
    <div>
      <div style="display:inline-block; padding: 0 0 0 2em;" >
        <input type="submit" id="dht-config-form_op_update" name="op.update-dht" value="Save" 
               onclick="YAHOO.mbi.prefmgr.submit('dht-config-form')"/>

      </div>
<!--
      <div style="display:inline-block; padding: 0 0 0 2em;" >
        <input type="submit" id="dht-config-form_op_defset" name="op.defset" value="Restore Defaults" 
              onclick="YAHOO.mbi.prefmgr.defset()"/>

      </div>
-->
    </div>
</form>




<script type="text/javascript">
  YAHOO.util.Event.addListener( window, "load",
                                YAHOO.mbi.prefmgr.init(
                                  { formid: "dht-config-form",
                                    viewUrl:"node-status?op.view=dht",
                                    updateUrl:"node-status?op.update-config=dht",  
                                    defsetUrl:"" }
                                 ));

</script>


