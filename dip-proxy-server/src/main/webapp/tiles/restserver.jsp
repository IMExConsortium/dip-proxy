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
             <s:iterator value="nativeRestServer.restServerContext.jsonConfig.restServer" status="serverMap">
                <s:set name="provider" value="key"/>
                <tr><td colspan="4" align="left"/><s:property value="#provider"/>:
                </td></tr> 
               
                <s:if test='value.size > 0'>    
                    <s:iterator value="value" status="serviceMap">
                        <s:set name="service" value="key"/>
                        
                        <tr><td colspan="4" align="left"/><s:property value="#service"/>:</td></tr>
                        
                        <tr><td colspan="4" align="left"/><s:property value="key"/>?<s:property value="value"/></td></tr> 

                        <s:if test='value.size > 0'>
                          <s:iterator value="value" status="restMap">
                            <tr>
                                <td align="left"/><s:property value="key"/>:</td>
                                <td align="left"/><s:textfield theme="simple"
                                                name="%{key}"
                                                value="%{value}"
                                                size="200"/>
                                </td>    
                                    
                          </s:iterator>  
                        </s:if>
                    </s:iterator> 
              </s:if>  
            </s:iterator>
         </s:if> 
        <tr><td colspan="4"><br></td></tr>
        
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



