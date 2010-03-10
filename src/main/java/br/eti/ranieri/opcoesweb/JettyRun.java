package br.eti.ranieri.opcoesweb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

public class JettyRun {
	private static final String LOCALHOST = "127.0.0.1";
	private static final int SHUTDOWN_PORT = 8079;
	private static Server server;

	public static void main(String[] args) throws Exception {
		
		try {
			Socket socket = new Socket(InetAddress.getByName(LOCALHOST), SHUTDOWN_PORT);
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			pw.print("\r\n");
			pw.flush();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		server = new Server();

		Connector connector = new SelectChannelConnector();
		connector.setPort(8080);
		server.setConnectors(new Connector[]{connector});

		WebAppContext webappcontext = new WebAppContext();
		webappcontext.setContextPath("/opcoesweb");
		webappcontext.setWar("./web");

		HandlerCollection handlers= new HandlerCollection();
		handlers.setHandlers(new Handler[]{webappcontext, new DefaultHandler()});

		server.setHandler(handlers);
		server.start();
		new MonitorThread().start();
		server.join();
	}
	
	private static class MonitorThread extends Thread {
		private ServerSocket socket;

        public MonitorThread() {
            setDaemon(true);
            setName("StopMonitor");
            try {
                socket = new ServerSocket(SHUTDOWN_PORT, 1, InetAddress.getByName(LOCALHOST));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            System.out.println("*** running jetty 'stop' thread");
            Socket accept;
            try {
                accept = socket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
                reader.readLine();
                System.out.println("*** stopping jetty embedded server");
                server.stop();
                accept.close();
                socket.close();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

	}
}
