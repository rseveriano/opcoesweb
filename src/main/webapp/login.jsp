<%@page language="java" contentType="text/html ; charset=UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<title>OpçõesWeb</title>
	</head>
	<body>
		<form action="<%= response.encodeURL(request.getContextPath() + "/j_spring_security_check") %>" method="POST">
			<table style="margin: 30% auto;">
				<tr>
					<td colspan="2"><% if (request.getParameter("error") != null) { %>
					<div wicket:id="feedback" style="font-weight: bold; color: darkred;">Usuário e/ou senha inválidos.</div>
					<% } %></td>
				</tr>
				<tr>
					<td align="right">Usuário:</td>
					<td><input type="text" name="j_username"/></td>
				</tr>
				<tr>
					<td align="right">Senha:</td>
					<td><input type="password" name="j_password"/></td>
				</tr>
				<tr>
					<td></td>
					<td><input type="submit" value="Acessar"/></td>
				</tr>
			</table>
		</form>
	</body>
</html>
