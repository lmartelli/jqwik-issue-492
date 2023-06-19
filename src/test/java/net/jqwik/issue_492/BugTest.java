package net.jqwik.issue_492;

import net.jqwik.api.*;
import net.jqwik.api.domains.Domain;
import net.jqwik.api.domains.DomainContext;
import net.jqwik.api.domains.DomainContextBase;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Domain(BugTest.ConfigurationDomain.class)
@PropertyDefaults(tries = 10)
public class BugTest {
    public abstract class Inner1<PARAMS> {
        @Property
        void genericProperty(@ForAll PARAMS params) {
            System.out.println("params = " + params);
            assertThat(params).isNotNull();
        }
    }

    @Group
    public class Inner2<T> extends Inner1<GenericType<T>> {
    }

    public record GenericType<T>(T value) {
    }

    @Group
    class Inner3 extends Inner2<String> {
    }

    @Domain(DomainContext.Global.class)
    static class ConfigurationDomain extends DomainContextBase {
        public class GenericTypeArbitraryProvider implements ArbitraryProvider {
            @Override
            public boolean canProvideFor(TypeUsage targetType) {
                return targetType.isOfType(BugTest.GenericType.class);
            }

            @Override
            public Set<Arbitrary<?>> provideFor(TypeUsage targetType, SubtypeProvider subtypeProvider) {
                System.out.println("provideFor " + targetType.getType().getTypeName());
                System.out.println("targetType=" + targetType);
                TypeUsage innerType = targetType.getTypeArguments().get(0);
                System.out.println("TypeArgument = " + innerType.getType());
                var innerTypeProviders = subtypeProvider.apply(innerType);
                if (innerTypeProviders.isEmpty())
                    throw new RuntimeException("Cannot find any Arbitrary provider for " + innerType.getType().getTypeName());
                return innerTypeProviders.stream()
                        .map(arbitrary -> arbitrary.map(BugTest.GenericType::new))
                        .collect(Collectors.toSet());
            }
        }
    }
}
