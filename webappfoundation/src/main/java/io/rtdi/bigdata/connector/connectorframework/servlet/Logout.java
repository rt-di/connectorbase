package io.rtdi.bigdata.connector.connectorframework.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/logout")
public class Logout extends HttpServlet {

	private static final long serialVersionUID = 754601115219128919L;

	public Logout() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter out = resp.getWriter();
		try {
			req.getSession(false).invalidate();
		} catch (IllegalStateException e) {
		}
		out.println("<!DOCTYPE html>");
		out.println("<html><head><meta http-equiv=\"refresh\" content=\"0; URL=index.html\"></head><body>");
		out.println("Redirecting to the <a href=\"index.html\">Index page</a></body></html>");
	}

}
