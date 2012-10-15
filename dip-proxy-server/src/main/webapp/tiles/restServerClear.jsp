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
           <s:if test='nativeRestServer.restServerContext.jsonConfig.restServer.size > 0'> 
             <s:iterator value="nativeRestServer.restServerContext.jsonConfig.restServer" status="serverMap">
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
                            <tr>
                                <td align="left"/><s:property value="key"/>:</td>
                                
                                <td align="left"/>
                                <s:if test="#service=='yeastmine' && key=='restUrl'">
                                    
                                    <s:textarea theme="simple"
                                                name="%{key}"
                                                value=""
                                                cols="100"
                                                rows="4"
                                                wrap="no"/>
                                </s:if>
                                <s:else>
                                    <s:textfield theme="simple"
                                                name="%{key}"
                                                value=""
                                                size="90"/>

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
                    <s:submit theme="simple"
                             type="button" value="Clear" 
                              label="CLEAR" name="buttonName"/>

                    <s:submit   theme="simple"
                                type="button" value="Update" 
                                label="UPDATE" name="buttonName"/>
            </td>
        </tr>

        <tr><td colspan="2"/><br></td></tr>

      </s:form>
    </table>



