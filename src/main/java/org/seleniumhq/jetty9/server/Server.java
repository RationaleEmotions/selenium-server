//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.seleniumhq.jetty9.server;

import org.seleniumhq.jetty9.http.*;
import org.seleniumhq.jetty9.http.HttpGenerator.CachedHttpField;
import org.seleniumhq.jetty9.server.handler.ContextHandler;
import org.seleniumhq.jetty9.server.handler.HandlerWrapper;
import org.seleniumhq.jetty9.util.*;
import org.seleniumhq.jetty9.util.annotation.ManagedAttribute;
import org.seleniumhq.jetty9.util.annotation.ManagedObject;
import org.seleniumhq.jetty9.util.annotation.Name;
import org.seleniumhq.jetty9.util.component.Graceful;
import org.seleniumhq.jetty9.util.component.LifeCycle;
import org.seleniumhq.jetty9.util.log.Log;
import org.seleniumhq.jetty9.util.log.Logger;
import org.seleniumhq.jetty9.util.thread.QueuedThreadPool;
import org.seleniumhq.jetty9.util.thread.ShutdownThread;
import org.seleniumhq.jetty9.util.thread.ThreadPool;
import org.seleniumhq.jetty9.util.thread.ThreadPool.SizedThreadPool;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@ManagedObject ("Jetty HTTP Servlet server")
public class Server extends HandlerWrapper implements Attributes {
    private static final Logger LOG = Log.getLogger(Server.class);
    private final AttributesMap _attributes;
    private final ThreadPool _threadPool;
    private final List<Connector> _connectors;
    private SessionIdManager _sessionIdManager;
    private boolean _stopAtShutdown;
    private boolean _dumpAfterStart;
    private boolean _dumpBeforeStop;
    private volatile Server.DateField _dateField;

    public Server() {
        this((ThreadPool) null);
    }

    public Server(@Name ("port") int port) {
        this((ThreadPool) null);
        ServerConnector connector = new ServerConnector(this);
        connector.setPort(port);
        this.setConnectors(new Connector[] {connector});
    }

    public Server(@Name ("address") InetSocketAddress addr) {
        this((ThreadPool) null);
        ServerConnector connector = new ServerConnector(this);
        connector.setHost(addr.getHostName());
        connector.setPort(addr.getPort());
        this.setConnectors(new Connector[] {connector});
    }

    public Server(@Name ("threadpool") ThreadPool pool) {
        this._attributes = new AttributesMap();
        this._connectors = new CopyOnWriteArrayList();
        this._dumpAfterStart = false;
        this._dumpBeforeStop = false;
        this._threadPool = (ThreadPool) (pool != null ? pool : new QueuedThreadPool());
        this.addBean(this._threadPool);
        this.setServer(this);
    }

    @ManagedAttribute ("version of this server")
    public static String getVersion() {
        return Jetty.VERSION;
    }

    public boolean getStopAtShutdown() {
        return this._stopAtShutdown;
    }

    public void setStopTimeout(long stopTimeout) {
        super.setStopTimeout(stopTimeout);
    }

    public void setStopAtShutdown(boolean stop) {
        if (stop) {
            if (! this._stopAtShutdown && this.isStarted()) {
                ShutdownThread.register(new LifeCycle[] {this});
            }
        } else {
            ShutdownThread.deregister(this);
        }

        this._stopAtShutdown = stop;
    }

    @ManagedAttribute (
        value = "connectors for this server",
        readonly = true
    )
    public Connector[] getConnectors() {
        ArrayList connectors = new ArrayList(this._connectors);
        return (Connector[]) connectors.toArray(new Connector[connectors.size()]);
    }

    public void addConnector(Connector connector) {
        if (connector.getServer() != this) {
            throw new IllegalArgumentException(
                "Connector " + connector + " cannot be shared among server " + connector.getServer() + " and server "
                    + this);
        } else {
            if (this._connectors.add(connector)) {
                this.addBean(connector);
            }

        }
    }

    public void removeConnector(Connector connector) {
        if (this._connectors.remove(connector)) {
            this.removeBean(connector);
        }

    }

    public void setConnectors(Connector[] connectors) {
        Connector[] oldConnectors;
        if (connectors != null) {
            oldConnectors = connectors;
            int len$ = connectors.length;

            for (int i$ = 0; i$ < len$; ++ i$) {
                Connector connector = oldConnectors[i$];
                if (connector.getServer() != this) {
                    throw new IllegalArgumentException(
                        "Connector " + connector + " cannot be shared among server " + connector.getServer()
                            + " and server " + this);
                }
            }
        }

        oldConnectors = this.getConnectors();
        this.updateBeans(oldConnectors, connectors);
        this._connectors.removeAll(Arrays.asList(oldConnectors));
        if (connectors != null) {
            this._connectors.addAll(Arrays.asList(connectors));
        }

    }

    @ManagedAttribute ("the server thread pool")
    public ThreadPool getThreadPool() {
        return this._threadPool;
    }

    @ManagedAttribute ("dump state to stderr after start")
    public boolean isDumpAfterStart() {
        return this._dumpAfterStart;
    }

    public void setDumpAfterStart(boolean dumpAfterStart) {
        this._dumpAfterStart = dumpAfterStart;
    }

    @ManagedAttribute ("dump state to stderr before stop")
    public boolean isDumpBeforeStop() {
        return this._dumpBeforeStop;
    }

    public void setDumpBeforeStop(boolean dumpBeforeStop) {
        this._dumpBeforeStop = dumpBeforeStop;
    }

    public HttpField getDateField() {
        long now = System.currentTimeMillis();
        long seconds = now / 1000L;
        Server.DateField df = this._dateField;
        if (df == null || df._seconds != seconds) {
            synchronized (this) {
                df = this._dateField;
                if (df == null || df._seconds != seconds) {
                    CachedHttpField field = new CachedHttpField(HttpHeader.DATE, DateGenerator.formatDate(now));
                    this._dateField = new Server.DateField(seconds, field);
                    return field;
                }
            }
        }

        return df._dateField;
    }

    protected void doStart() throws Exception {
        if (this.getStopAtShutdown()) {
            ShutdownThread.register(new LifeCycle[] {this});
        }

        ShutdownMonitor.register(new LifeCycle[] {this});
        ShutdownMonitor.getInstance().start();
        LOG.info("jetty-" + getVersion(), new Object[0]);
        HttpGenerator.setJettyVersion(HttpConfiguration.SERVER_VERSION);
        MultiException mex = new MultiException();
        SizedThreadPool pool = (SizedThreadPool) this.getBean(SizedThreadPool.class);
        int max = pool == null ? - 1 : pool.getMaxThreads();
        int selectors = 0;
        int acceptors = 0;
        if (mex.size() == 0) {
            Iterator needed = this._connectors.iterator();

            while (needed.hasNext()) {
                Connector i$ = (Connector) needed.next();
                if (i$ instanceof AbstractConnector) {
                    acceptors += ((AbstractConnector) i$).getAcceptors();
                }

                if (i$ instanceof ServerConnector) {
                    selectors += ((ServerConnector) i$).getSelectorManager().getSelectorCount();
                }
            }
        }

        int needed1 = 1 + selectors + acceptors;
        if (max > 0 && needed1 > max) {
            throw new IllegalStateException(String
                .format("Insufficient threads: max=%d < needed(acceptors=%d + selectors=%d + request=1)",
                    new Object[] {Integer.valueOf(max), Integer.valueOf(acceptors), Integer.valueOf(selectors)}));
        } else {
            try {
                super.doStart();
            } catch (Throwable var11) {
                mex.add(var11);
            }

            Iterator i$1 = this._connectors.iterator();

            while (i$1.hasNext()) {
                Connector connector = (Connector) i$1.next();

                try {
                    connector.start();
                } catch (Throwable var10) {
                    mex.add(var10);
                }
            }

            if (this.isDumpAfterStart()) {
                this.dumpStdErr();
            }

            mex.ifExceptionThrow();
            LOG.info(String.format("Started @%dms", new Object[] {Long.valueOf(Uptime.getUptime())}), new Object[0]);
        }
    }

    protected void start(LifeCycle l) throws Exception {
        if (! (l instanceof Connector)) {
            super.start(l);
        }

    }

    protected void doStop() throws Exception {
        if (this.isDumpBeforeStop()) {
            this.dumpStdErr();
        }

        MultiException mex = new MultiException();
        ArrayList futures = new ArrayList();
        Iterator gracefuls = this._connectors.iterator();

        while (gracefuls.hasNext()) {
            Connector arr$ = (Connector) gracefuls.next();
            futures.add(arr$.shutdown());
        }

        Handler[] var18 = this.getChildHandlersByClass(Graceful.class);
        Handler[] var19 = var18;
        int len$ = var18.length;

        for (int e = 0; e < len$; ++ e) {
            Handler connector = var19[e];
            futures.add(((Graceful) connector).shutdown());
        }

        long stopTimeout = this.getStopTimeout();
        if (stopTimeout > 0L) {
            long stop_by = System.currentTimeMillis() + stopTimeout;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Graceful shutdown {} by ", new Object[] {this, new Date(stop_by)});
            }

            Iterator e1 = futures.iterator();

            while (e1.hasNext()) {
                Future future = (Future) e1.next();

                try {
                    if (! future.isDone()) {
                        future.get(Math.max(1L, stop_by - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                    }
                } catch (Exception var17) {
                    mex.add(var17);
                }
            }
        }

        Iterator var20 = futures.iterator();

        while (var20.hasNext()) {
            Future var21 = (Future) var20.next();
            if (! var21.isDone()) {
                var21.cancel(true);
            }
        }

        var20 = this._connectors.iterator();

        while (var20.hasNext()) {
            Connector var22 = (Connector) var20.next();

            try {
                var22.stop();
            } catch (Throwable var16) {
                mex.add(var16);
            }
        }

        try {
            super.doStop();
        } catch (Throwable var15) {
            mex.add(var15);
        }

        if (this.getStopAtShutdown()) {
            ShutdownThread.deregister(this);
        }

        ShutdownMonitor.deregister(this);
        mex.ifExceptionThrow();
    }

    //This is the overridden method wherein we are filtering out TRACE calls and then setting their
    //Error code to 403 Forbidden.
    public void handle(HttpChannel<?> connection) throws IOException, ServletException {
        String target = connection.getRequest().getPathInfo();
        Request request = connection.getRequest();
        Response response = connection.getResponse();
        if (HttpMethod.TRACE.is(request.getMethod())) {
            response.sendError(403, request.getMethod() + " is explicitly disabled.");

        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(request.getDispatcherType() + " " + request.getMethod() + " " + target + " on " + connection,
                new Object[0]);
        }

        if (! HttpMethod.OPTIONS.is(request.getMethod()) && ! "*".equals(target)) {
            this.handle(target, request, request, response);
        } else {
            if (! HttpMethod.OPTIONS.is(request.getMethod())) {
                response.sendError(400);
            }

            this.handleOptions(request, response);
            if (! request.isHandled()) {
                this.handle(target, request, request, response);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                "RESPONSE " + target + "  " + connection.getResponse().getStatus() + " handled=" + request.isHandled(),
                new Object[0]);
        }
    }

    protected void handleOptions(Request request, Response response) throws IOException {
    }

    public void handleAsync(HttpChannel<?> connection) throws IOException, ServletException {
        HttpChannelState state = connection.getRequest().getHttpChannelState();
        AsyncContextEvent event = state.getAsyncContextEvent();
        Request baseRequest = connection.getRequest();
        String path = event.getPath();
        if (path != null) {
            ServletContext target = event.getServletContext();
            HttpURI request = new HttpURI(URIUtil.addPaths(target == null ? null : target.getContextPath(), path));
            baseRequest.setUri(request);
            baseRequest.setRequestURI((String) null);
            baseRequest.setPathInfo(request.getDecodedPath());
            if (request.getQuery() != null) {
                baseRequest.mergeQueryParameters(request.getQuery(), true);
            }
        }

        String target1 = baseRequest.getPathInfo();
        HttpServletRequest request1 = (HttpServletRequest) event.getSuppliedRequest();
        HttpServletResponse response = (HttpServletResponse) event.getSuppliedResponse();
        if (LOG.isDebugEnabled()) {
            LOG.debug(request1.getDispatcherType() + " " + request1.getMethod() + " " + target1 + " on " + connection,
                new Object[0]);
            this.handle(target1, baseRequest, request1, response);
            LOG.debug("RESPONSE " + target1 + "  " + connection.getResponse().getStatus(), new Object[0]);
        } else {
            this.handle(target1, baseRequest, request1, response);
        }

    }

    public void join() throws InterruptedException {
        this.getThreadPool().join();
    }

    public SessionIdManager getSessionIdManager() {
        return this._sessionIdManager;
    }

    public void setSessionIdManager(SessionIdManager sessionIdManager) {
        this.updateBean(this._sessionIdManager, sessionIdManager);
        this._sessionIdManager = sessionIdManager;
    }

    public void clearAttributes() {
        Enumeration names = this._attributes.getAttributeNames();

        while (names.hasMoreElements()) {
            this.removeBean(this._attributes.getAttribute((String) names.nextElement()));
        }

        this._attributes.clearAttributes();
    }

    public Object getAttribute(String name) {
        return this._attributes.getAttribute(name);
    }

    public Enumeration<String> getAttributeNames() {
        return AttributesMap.getAttributeNamesCopy(this._attributes);
    }

    public void removeAttribute(String name) {
        Object bean = this._attributes.getAttribute(name);
        if (bean != null) {
            this.removeBean(bean);
        }

        this._attributes.removeAttribute(name);
    }

    public void setAttribute(String name, Object attribute) {
        this.addBean(attribute);
        this._attributes.setAttribute(name, attribute);
    }

    public URI getURI() {
        NetworkConnector connector = null;
        Iterator context = this._connectors.iterator();

        while (context.hasNext()) {
            Connector e = (Connector) context.next();
            if (e instanceof NetworkConnector) {
                connector = (NetworkConnector) e;
                break;
            }
        }

        if (connector == null) {
            return null;
        } else {
            ContextHandler context1 = (ContextHandler) this.getChildHandlerByClass(ContextHandler.class);

            try {
                String e1 = connector.getDefaultConnectionFactory().getProtocol().startsWith("SSL-") ? "https" : "http";
                String host = connector.getHost();
                if (context1 != null && context1.getVirtualHosts() != null && context1.getVirtualHosts().length > 0) {
                    host = context1.getVirtualHosts()[0];
                }

                if (host == null) {
                    host = InetAddress.getLocalHost().getHostAddress();
                }

                String path = context1 == null ? null : context1.getContextPath();
                if (path == null) {
                    path = "/";
                }

                return new URI(e1, (String) null, host, connector.getLocalPort(), path, (String) null, (String) null);
            } catch (Exception var6) {
                LOG.warn(var6);
                return null;
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
    }

    public void dump(Appendable out, String indent) throws IOException {
        this.dumpBeans(out, indent,
            new Collection[] {Collections.singleton(new ClassLoaderDump(this.getClass().getClassLoader()))});
    }

    public static void main(String... args) throws Exception {
        System.err.println(getVersion());
    }

    private static class DateField {
        final long _seconds;
        final HttpField _dateField;

        public DateField(long seconds, HttpField dateField) {
            this._seconds = seconds;
            this._dateField = dateField;
        }
    }
}
