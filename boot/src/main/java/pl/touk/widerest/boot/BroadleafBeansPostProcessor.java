package pl.touk.widerest.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import javax.servlet.Filter;
import java.util.Arrays;

public class BroadleafBeansPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        registerAliases(beanFactory);
        removeWebFilters((DefaultListableBeanFactory) beanFactory);

        if (beanFactory.containsBeanDefinition("jpaVendorAdapter")) {
            ((DefaultListableBeanFactory) beanFactory).removeBeanDefinition("blJpaVendorAdapter");
            beanFactory.registerAlias("jpaVendorAdapter", "blJpaVendorAdapter");
        }
    }

    private static void registerAliases(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.registerAlias("dataSource", "webDS");
        beanFactory.registerAlias("dataSource", "webStorageDS");
        beanFactory.registerAlias("dataSource", "webSecureDS");
    }

    private static void removeWebFilters(DefaultListableBeanFactory beanFactory) {
        Arrays.stream(beanFactory.getBeanNamesForType(Filter.class))
                .filter(beanName -> {
                    final String beanClassName = beanFactory.getBeanDefinition(beanName).getBeanClassName();
                    return beanClassName != null && beanClassName.startsWith("org.broadleafcommerce");
                })
                .forEach(beanFactory::removeBeanDefinition);

    }
}
