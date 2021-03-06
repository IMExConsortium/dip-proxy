<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

    <br><br>
    <table align="center">
      <s:form theme="simple">
        <caption>
            <center>Update Json Configuration File</center>
        </caption>

        <tr><td colspan="2"/><br></td></tr>

        <ul>
            <s:set var="contextTopMap" value="contextMap.get(contextTop)"/>  

           <s:if test='#contextTopMap.size > 0'> 
             <s:iterator value="#contextTopMap" status="serverMap">
                <s:set var="provider" value="key"/>
                <tr><td colspan="2" align="left"/><li>provider:<s:property value="#provider"/></li>
                </td></tr> 
               
                <s:if test='value.size > 0'>    

                    <s:set var="providerSize" value="value.size"/> 

                    <s:iterator value="value" status="serviceMap">
                        <s:set var="service" value="key"/>
                        
                        <tr><td colspan="2" align="left"/>
                            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;service:<s:property value="#service"/>
                        </td></tr>
                        
                        <s:if test='value.size > 0'>
                          <s:iterator value="value" status="restMap">

                            <s:set var="oppKey" value=" #provider + '_' + #service + '_' +  key"/>

                            <s:set var="oppPropName" value="'opp.' + #oppKey"/>
                            <tr>
                                <td align="right"/><s:property value="key"/>:</td>
                                 
                                <td align="left"/>
                                <s:if test="value.length() > 110 ">
                                    <s:set var="colNum" value="value.length() / 100 + 1 "/>
                                    <s:textarea theme="simple"
                                                name="%{#oppPropName}"
                                                value="%{value}"
                                                cols="100"
                                                rows="%{#colNum}"
                                                wrap="no"/>
                                </s:if>
                                <s:else>
                                    <s:textfield theme="simple"
                                                name="%{#oppPropName}"
                                                value="%{value}"
                                                size="%{value.length()+ 2}"/>
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
        
      </s:form>
    </table>



