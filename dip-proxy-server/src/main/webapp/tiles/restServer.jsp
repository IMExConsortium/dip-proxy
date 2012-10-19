<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

    <br><br>
    <table>
      <s:form theme="simple" action="native-configure">
        <s:hidden name="id" value="%{id}"/>

        <caption>
            <center>Update Json Configuration File for Native Rest Server</center>
        </caption>

        <tr><td colspan="2"/><br></td></tr>

        <ul>
           <s:set name="restServer" value="nativeRestServer.restServerContext.jsonConfig.restServer"/>
           <s:if test='#restServer.size > 0'> 
             <s:iterator value="#restServer" status="serverMap">
                <s:set name="provider" value="key"/>
                <tr><td colspan="2" align="left"/><li><s:property value="#provider"/></li>
                </td></tr> 
               
                <s:if test='value.size > 0'>    

                    <s:set name="providerSize" value="value.size"/> 

                    <s:iterator value="value" status="serviceMap">
                        <s:set name="service" value="key"/>
                        
                        <tr><td colspan="2" align="center"/>
                            <s:property value="#service"/>
                        </td></tr>
                        
                        <s:if test='value.size > 0'>
                          <s:iterator value="value" status="restMap">
                            <s:set name="propName" 
                                   value="'nativeRestServer.restServerContext.jsonConfig.restServer.'+ 
                                          #provider + '.'+ #service + '.' + key"/>

                            <tr>
                                <td align="left"/><s:property value="key"/>:</td>
                                
                                <td align="left"/>
                                <s:if test="#service=='yeastmine' && key=='restUrl'">
                                    
                                    <s:textarea theme="simple"
                                                name="%{#propName}"
                                                value="%{value[0]}"
                                                cols="100"
                                                rows="4"
                                                wrap="no"/>
                                </s:if>
                                <s:else>
                                    <s:textfield theme="simple"
                                                name="%{#propName}"
                                                value="%{value[0]}"
                                                size="95"/>

                                </s:else>
                                </td>    
                                    
                          </s:iterator>  
                        </s:if>

                        <s:if test='#providerSize > 1'>
                            <tr><td colspan="2"/><br></td></tr>
                        </s:if>

                    </s:iterator> 

                </s:if> 

                <tr><td colspan="2"/><br></td></tr>

            </s:iterator>

         </s:if> 
         </ul>
        
        <tr>
            <td align="center" colspan="2">
                    <s:submit name="op.clear" value="Clear" />
                    <s:submit name="op.update" value="Update"/>
            </td>

        </tr>

        <tr><td colspan="2"/><br></td></tr>

      </s:form>
    </table>



