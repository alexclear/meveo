package org.meveo.service.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.meveo.model.billing.BillingAccount;
import org.meveo.model.filter.AndCompositeFilterCondition;
import org.meveo.model.filter.Filter;
import org.meveo.model.filter.FilterCondition;
import org.meveo.model.filter.FilterSelector;
import org.meveo.model.filter.NativeFilterCondition;
import org.meveo.model.filter.OrCompositeFilterCondition;
import org.meveo.model.filter.OrderCondition;
import org.meveo.model.filter.PrimitiveFilterCondition;
import org.meveo.util.MeveoJpaForJobs;

/**
 * @author Edward P. Legaspi
 **/
@RunWith(Arquillian.class)
public class FilteredQueryBuilderTest extends BaseFilterTest {

	@Inject
	@MeveoJpaForJobs
	private EntityManager em;

	@Inject
	private FilterService filterService;

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive result = ShrinkWrap.create(WebArchive.class, "filter.war");

		result = initArchive(result);

		return result;
	}

	@Test
	public void elMatchTest() {
		NativeFilterCondition nativeFilterCondition = new NativeFilterCondition();
		nativeFilterCondition.setFilterConditionType(NativeFilterCondition.class
				.getAnnotation(DiscriminatorValue.class).value());
		nativeFilterCondition.setEl("#{ba.code eq 'BA1'}");

		BillingAccount ba = new BillingAccount();
		ba.setCode("BA1");

		Map<Object, Object> params = new HashMap<>();
		params.put("ba", ba);

		Assert.assertEquals(true, filterService.isMatch(nativeFilterCondition, params));
	}

	@Test
	public void elNotMatchTest() {
		NativeFilterCondition nativeFilterCondition = new NativeFilterCondition();
		nativeFilterCondition.setFilterConditionType(NativeFilterCondition.class
				.getAnnotation(DiscriminatorValue.class).value());
		nativeFilterCondition.setEl("#{ba.code eq 'BA2'}");

		BillingAccount ba = new BillingAccount();
		ba.setCode("BA1");

		Map<Object, Object> params = new HashMap<>();
		params.put("ba", ba);

		Assert.assertNotEquals(true, filterService.isMatch(nativeFilterCondition, params));
	}

	@Test
	public void filterTest() {
		AndCompositeFilterCondition andCompositeFilterCondition = new AndCompositeFilterCondition();
		andCompositeFilterCondition.setFilterConditionType(AndCompositeFilterCondition.class.getAnnotation(
				DiscriminatorValue.class).value());
		Set<FilterCondition> andFilterConditions = new HashSet<>();

		NativeFilterCondition nativeFilterCondition = new NativeFilterCondition();
		nativeFilterCondition.setFilterConditionType(NativeFilterCondition.class
				.getAnnotation(DiscriminatorValue.class).value());
		nativeFilterCondition.setJpql("c.countryCode like '%A%'");
		andFilterConditions.add(nativeFilterCondition);

		/* OR */
		OrCompositeFilterCondition orCompositeFilterCondition = new OrCompositeFilterCondition();
		orCompositeFilterCondition.setFilterConditionType(OrCompositeFilterCondition.class.getAnnotation(
				DiscriminatorValue.class).value());
		Set<FilterCondition> orFilterConditions = new HashSet<>();

		NativeFilterCondition nativeFilterCondition2 = new NativeFilterCondition();
		nativeFilterCondition2.setFilterConditionType(NativeFilterCondition.class.getAnnotation(
				DiscriminatorValue.class).value());
		nativeFilterCondition2.setJpql("c.countryCode like 'B%'");
		orFilterConditions.add(nativeFilterCondition2);

		PrimitiveFilterCondition primitiveFilterCondition = new PrimitiveFilterCondition();
		primitiveFilterCondition.setFilterConditionType(PrimitiveFilterCondition.class.getAnnotation(
				DiscriminatorValue.class).value());
		primitiveFilterCondition.setFieldName("c.countryCode");
		primitiveFilterCondition.setOperator("like");
		primitiveFilterCondition.setOperand("C%");
		orFilterConditions.add(primitiveFilterCondition);

		PrimitiveFilterCondition primitiveFilterCondition2 = new PrimitiveFilterCondition();
		primitiveFilterCondition2.setFilterConditionType(PrimitiveFilterCondition.class.getAnnotation(
				DiscriminatorValue.class).value());
		primitiveFilterCondition2.setFieldName("c.id");
		primitiveFilterCondition2.setOperator(">");
		primitiveFilterCondition2.setOperand("100");
		orFilterConditions.add(primitiveFilterCondition2);
		orCompositeFilterCondition.setFilterConditions(orFilterConditions);
		/* OR */

		andFilterConditions.add(orCompositeFilterCondition);
		andCompositeFilterCondition.setFilterConditions(andFilterConditions);

		OrderCondition orderCondition = new OrderCondition();
		orderCondition.setAscending(false);
		orderCondition.setFieldNames(new ArrayList<>(Arrays.asList("countryCode")));

		FilterSelector filterSelector1 = new FilterSelector();
		filterSelector1.setTargetEntity("org.meveo.model.billing.Country");
		filterSelector1.setAlias("c");
		filterSelector1.setDisplayFields(new ArrayList<>(Arrays.asList("id", "countryCode", "descriptionEn",
				"currency", "language")));

		Filter filter = new Filter();
		filter.setFilterCondition(andCompositeFilterCondition);
		filter.setPrimarySelector(filterSelector1);
		// filter.setSecondarySelectors(filterSelectors);
		filter.setOrderCondition(orderCondition);

		try {
			String result = filterService.filteredList(filter, 1, 2,null);

			System.out.println(result);

			Assert.assertNotNull(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
