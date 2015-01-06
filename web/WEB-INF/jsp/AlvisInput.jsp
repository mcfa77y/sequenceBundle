<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Alvis configuration xx</title>
    </head>
    <body>
        <div align="center">
            <form:form action="/alvis-web-interface/Alvis" method="post" commandName="userForm">
                <table border="0">
                    <tr>
                        <td colspan="2" align="center"><h2>Alvis configuration</h2></td>
                    </tr>
                    <tr>
                        <td colspan="1" align="center">
                            <p>Sequences:</p>
                            <form:textarea path="sequences"/>
                        </td>
                        <td><springForm:errors path="sequences" cssClass="error" /></td>
                    </tr>
                    <tr>
                        <td colspan="2"><br><br></td>
                    </tr>
                    <tr>
                        <td>Cell width:</td>
                        <td><form:input path="cellWidth" /></td>
                    </tr>
                    <tr>
                        <td>Cell height:</td>
                        <td><form:input path="cellHeight" /></td>
                    </tr>
                    <tr>
                        <td>Maximum stack height:</td>
                        <td><form:input path="maxBundleWidth" /></td>
                    </tr>
                    <tr>
                        <td>Bundle curvature left:</td>
                        <td><form:input path="tangL" /></td>
                    </tr>
                    <tr>
                        <td>Bundle curvature right:</td>
                        <td><form:input path="tangR" /></td>
                    </tr>
                    <tr>
                        <td>Base width:</td>
                        <td><form:input path="horizontalExtent" /></td>
                    </tr>
                    <tr>
                        <td>Global Y-Axis offset:</td>
                        <td><form:input path="offsetY" /></td>
                    </tr>
                    <tr>
                        <td>Conservation threshold:</td>
                        <td><form:input path="conservationThreshold" /></td>
                    </tr>
                    <tr>
                        <td>Minimum transparency per thread:</td>
                        <td><form:input path="minAlphaPerThread" /></td>
                    </tr>
                    <tr>
                        <td>Maximum total transparency:</td>
                        <td><form:input path="maxAlphaTotal" /></td>
                    </tr>
                    <tr>
                        <td>Group stacking:</td>
                        <td><form:select path="groupStacking" items="${groupStackingList}" /></td>
                    </tr>
                    <tr>
                        <td>Gap rendering:</td>
                        <td><form:select path="gapRendering" items="${gapRenderingList}" /></td>
                    </tr>
                    <tr>
                        <td>Show consensus sequence:</td>
                        <td><form:checkbox path="showingConsensus" /></td>
                    </tr>
                    <tr>
                        <td>Show horizontal grid lines:</td>
                        <td><form:checkbox path="showingHorizontalLines" /></td>
                    </tr>
                    <tr>
                        <td>Show vertical grid lines:</td>
                        <td><form:checkbox path="showingVerticalLines" /></td>
                    </tr>
                    <tr>
                        <td>Show alphabet overlay:</td>
                        <td><form:checkbox path="showingOverlay" /></td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center"><input type="submit" value="Submit" /></td>
                    </tr>
                </table>
            </form:form>
        </div>
    </body>
</html>