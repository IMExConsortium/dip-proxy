<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

    <br><br>
    <table>
      <s:form theme="simple" action="native-configure">
        <s:hidden name="id" value="%{id}"/>
        <caption>
            Update Json Configuration File for Rest Server
        </caption>
        <tr><td><br></td></tr>
        
        <s:if test='nativeRestServer.restServerContext.jsonConfig.restServer.size > 0'>
            <s:iterator value="nativeRestServer.restServerContext.jsonConfig.restServer" id="server">
                <s:set name="provider" value="#server.label"/>
                <s:set name="serviceMap" value="#server.value"/>
                <tr><td colspan="4" align="left"/><s:property value="#provider"/>:</td></tr>
                
                <s:if test='#serviceMap.size >  0 ' >
                    <s:iterator value="#serviceMap" id="service">
                        <s:set name="service" value="#service.label"/>
                        <s:set name="restMap" value="#service.value"/>
                        <tr><td colspan="4" align="left"/><s:property value="#service"/>:</td></tr>
                        <tr>
                        <s:if test='#restMap.size > 0 '>  
                            <s:iterator value="#restMap" id="rest">
                                <s:set name="label" value="#rest.label"/>
                                <s:set name="value" value="#rest.value"/>
                            
                                <s:if test="#label=='restUrl'">
                                    <s:set name="restUrl" value="#value"/>
                                    <td align="left"/><s:property value="#restUrl"/>:</td>
                                    <td align="left"/><s:textfield theme="simple"
                                                                   name="%{#restUrl}"
                                                                   value="%{#restUrl}"
                                                                   size="200"/>
                                    </td>    
                                </s:if>

                                <s:if test="#label=='restAcTag'">
                                    <s:set name="restAcTag" value="#value"/>
                                    <td align="left"/>restAcTag:</td>
                                    <td align="left"/><s:textfield theme="simple"
                                                                   name="%{#restAcTag}"
                                                                   value="%{#restAcTag}"
                                                                   size="200"/>
                                    </td>
                                </s:if>
                            </s:iterator>
                        </s:if>  
                    </s:iterator>
                </s:if>
            </s:iterator>
        </s:if>
        <tr><td colspan="4"><br></td></tr>
        
        <%--
        <s:if test='nativeRestServer.serverContext.jsonConfig.serverLocation.size > 0'>
         <s:iterator value="wSConfig.serverContext.jsonConfig.serverLocation" id="location">
          <s:set name="label" value="#location.label"/>
          <s:set name="value" value="#location.value"/>
          <s:set name="propname" value="'wSConfig.' + #label"/>
            <tr><td align="right">
                <s:property value="#label"/>:</td>
            <td align="left">
                <s:textfield    theme="simple"
                                name="%{#propname}"
                                value="%{#value}"
                                size="40"/>

            </td>
         </s:iterator>
        </s:if>
        --%>
        <tr><td colspan="4"><br><br></td></tr>
        <tr>
            <td align="center" colspan="4">
                    <s:submit theme="simple"
                             type="button" value="Clear" 
                              label="CLEAR" name="buttonName"/>

                    <s:submit   theme="simple"
                                type="button" value="Update" 
                                label="UPDATE" name="buttonName"/>
            </td>
        </tr>
      </s:form>
    </table>



