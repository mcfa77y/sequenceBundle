<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>   
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Error</title>
</head>
<body>
    <div align="center">
        <table border="0">
            <tr>
                <td colspan="2" align="center"><h2>Error processing input!</h2></td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <p>${userForm.errorMessage}</p>
                </td>
            </tr>
            <tr>
                <td colspan="2" align="center"><FORM><INPUT Type="button" VALUE="Back" onClick="history.go(-1);return true;"></FORM></td>
            </tr>
        </table>
    </div>
</body>
</html>