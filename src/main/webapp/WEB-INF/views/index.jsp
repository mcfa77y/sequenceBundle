<%--
    Document   : SBWeb
    Created on : Dec 30, 2014, 9:42:11 PM
    Author     : joelau
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SB Web Page</title>

<%@include file="scripts-styles.jspf"%>

<script>
	try {
		Typekit.load();
	} catch (e) {
	}
</script>
</head>
<body>

	<%@include file="header.jspf"%>

	<hr>
	<div class="container">
		<div class="row">
			<div class="col-md-6 col-md-offset-3 summary-heading center">Sequence
				Bundles web tool allows you to rapidly 3 visualise protein sequence
				alignments, experiment with basic visualisation settings and
				discover hidden sequence motifs.</div>
		</div>
		
		<div class="panel-group" id="accordion" role="tablist"
			aria-multiselectable="false">
			<%@include file="upload.jspf"%>

			<%@include file="visualize.jspf"%>

			<%@include file="download-share.jspf"%>
		</div>
	</div>

	<%@include file="footer.jspf"%>


</body>
</html>
