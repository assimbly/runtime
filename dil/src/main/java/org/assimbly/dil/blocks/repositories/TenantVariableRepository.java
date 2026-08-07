package org.assimbly.dil.blocks.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.StreamCache;
import org.apache.camel.StreamCacheException;
import org.apache.camel.spi.BrowsableVariableRepository;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.VariableRepository;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.StringHelper;

/**
 * Tenant-scoped {@link VariableRepository}, stored in-memory per tenant id.
 * <p/>
 * Sits between {@code group} and {@code global} in scope: a tenant can span
 * multiple named groups, but variables set here are not visible outside the
 * tenant, and are wider than any single group or route.
 * <p/>
 * Variable names use the syntax {@code tenant:<tenantId>:<key>}, e.g.
 * <pre>
 * exchange.setVariable("tenant:acme:myKey", someValue);
 * Object val = exchange.getVariable("tenant:acme:myKey");
 * </pre>
 * Modeled directly on Camel's own {@code RouteVariableRepository} /
 * {@code GroupVariableRepository} so it behaves consistently with the
 * built-in repositories (including participating in stream-caching).
 */
public final class TenantVariableRepository extends ServiceSupport implements BrowsableVariableRepository, CamelContextAware {

    /**
     * Registry bean id under which this repository must be bound so Camel's
     * {@code VariableRepositoryFactory} can discover it (lookup is by id ==
     * repository id, i.e. "tenant").
     */
    public static final String TENANT_VARIABLE_REPOSITORY_ID = "tenant";

    private final Map<String, Map<String, Object>> tenants = new ConcurrentHashMap<>();
    private CamelContext camelContext;
    private StreamCachingStrategy strategy;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public String getId() {
        return TENANT_VARIABLE_REPOSITORY_ID;
    }

    @Override
    public Object getVariable(String name) {
        String tenantId = StringHelper.before(name, ":");
        String key = StringHelper.after(name, ":");
        if (tenantId == null || key == null) {
            throw new IllegalArgumentException("Name must be tenantId:name syntax");
        }
        Object answer = null;
        Map<String, Object> variables = tenants.get(tenantId);
        if (variables != null) {
            answer = variables.get(key);
        }
        if (answer instanceof StreamCache sc) {
            // reset so the cache is ready to be used as a variable
            sc.reset();
        }
        return answer;
    }

    @Override
    public void setVariable(String name, Object value) {
        String tenantId = StringHelper.before(name, ":");
        String key = StringHelper.after(name, ":");
        if (tenantId == null || key == null) {
            throw new IllegalArgumentException("Name must be tenantId:name syntax");
        }

        if (value != null && strategy != null) {
            StreamCache sc = convertToStreamCache(value);
            if (sc != null) {
                value = sc;
            }
        }
        if (value != null) {
            Map<String, Object> variables = tenants.computeIfAbsent(tenantId, s -> new ConcurrentHashMap<>(8));
            variables.put(key, value);
        } else {
            // if the value is null, we just remove the key from the map
            Map<String, Object> variables = tenants.get(tenantId);
            if (variables != null) {
                variables.remove(key);
            }
        }
    }

    @Override
    public Object removeVariable(String name) {
        String tenantId = StringHelper.before(name, ":");
        String key = StringHelper.after(name, ":");
        if (tenantId == null || key == null) {
            throw new IllegalArgumentException("Name must be tenantId:name syntax");
        }

        Map<String, Object> variables = tenants.get(tenantId);
        if (variables != null) {
            if ("*".equals(key)) {
                variables.clear();
                return null;
            } else {
                return variables.remove(key);
            }
        }
        return null;
    }

    /**
     * Removes all variables for a given tenant, e.g. when a tenant is
     * offboarded. Not part of {@link VariableRepository}; call directly.
     */
    public void removeTenant(String tenantId) {
        tenants.remove(tenantId);
    }

    public boolean hasVariables() {
        for (var vars : tenants.values()) {
            if (!vars.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        int size = 0;
        for (var vars : tenants.values()) {
            size += vars.size();
        }
        return size;
    }

    public Stream<String> names() {
        List<String> answer = new ArrayList<>();
        for (var tenantEntry : tenants.entrySet()) {
            for (var e : tenantEntry.getValue().entrySet()) {
                answer.add(tenantEntry.getKey() + ":" + e.getKey());
            }
        }
        return answer.stream();
    }

    public Map<String, Object> getVariables() {
        Map<String, Object> answer = new ConcurrentHashMap<>();
        for (var tenantEntry : tenants.entrySet()) {
            for (var e : tenantEntry.getValue().entrySet()) {
                answer.put(tenantEntry.getKey() + ":" + e.getKey(), e.getValue());
            }
        }
        return answer;
    }

    public void clear() {
        tenants.clear();
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        if (camelContext != null && camelContext.isStreamCaching()) {
            strategy = camelContext.getStreamCachingStrategy();
        }
    }

    private StreamCache convertToStreamCache(Object body) {
        if (body == null) {
            return null;
        } else if (body instanceof StreamCache sc) {
            sc.reset();
            return sc;
        }
        return tryStreamCache(body);
    }

    private StreamCache tryStreamCache(Object body) {
        try {
            return strategy.cache(body);
        } catch (Exception e) {
            throw new StreamCacheException(body, e);
        }
    }
}