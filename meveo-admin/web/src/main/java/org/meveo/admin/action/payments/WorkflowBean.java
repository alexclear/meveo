/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.admin.action.payments;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.jboss.seam.international.status.builder.BundleKey;
import org.meveo.admin.action.BaseBean;
import org.meveo.admin.action.admin.ViewBean;
import org.meveo.admin.action.admin.custom.GroupedDecisionRule;
import org.meveo.admin.exception.BusinessException;
import org.meveo.admin.web.interceptor.ActionMethod;
import org.meveo.admin.wf.WorkflowType;
import org.meveo.admin.wf.WorkflowTypeClass;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ReflectionUtils;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.shared.DateUtils;
import org.meveo.model.wf.DecisionRuleTypeEnum;
import org.meveo.model.wf.WFAction;
import org.meveo.model.wf.WFDecisionRule;
import org.meveo.model.wf.WFTransition;
import org.meveo.model.wf.Workflow;
import org.meveo.service.base.PersistenceService;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.wf.WFActionService;
import org.meveo.service.wf.WFDecisionRuleService;
import org.meveo.service.wf.WFTransitionService;
import org.meveo.service.wf.WorkflowService;
import org.omnifaces.cdi.ViewScoped;

/**
 * Standard backing bean for {@link Workflow} (extends {@link BaseBean} that provides almost all common methods to handle entities filtering/sorting in datatable, their create,
 * edit, view, delete operations). It works with Manaty custom JSF components.
 */
@Named
@ViewScoped
@ViewBean
public class WorkflowBean extends BaseBean<Workflow> {

    private static final long serialVersionUID = 1L;
    private static final String LESS_SEPARATOR = " < ";
    private static final String LESS_SEPARATOR_NO_SPACE_LEFT = "< ";
    private static final String LESS_SEPARATOR_NO_SPACE_RIGHT = " <";

    /**
     * Injected @{link Workflow} service. Extends {@link PersistenceService}.
     */
    @Inject
    private WorkflowService workflowService;

    @Inject
    private WFTransitionService wFTransitionService;

    @Inject
    private WFDecisionRuleService wfDecisionRuleService;

    @Inject
    private WFActionService wfActionService;

    private List<String> wfTransitionRulesName;

    private List<List<WFDecisionRule>> wfDecisionRulesByName = new ArrayList<>();

    private List<GroupedDecisionRule> selectedRules = new ArrayList<>();

    private List<WFAction> wfActions = new ArrayList<>();

    private boolean showDetailPage = false;

    // @Produces
    // @Named
    private transient WFTransition wfTransition = new WFTransition();

    private transient WFDecisionRule newWFDecisionRule = new WFDecisionRule();

    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public WorkflowBean() {
        super(Workflow.class);
    }

    @Override
    public Workflow initEntity() {
        super.initEntity();
     //   PersistenceUtils.initializeAndUnproxy(entity.getActions());
        return entity;
    }

    public WFTransition getWfTransition() {
        return wfTransition;
    }

    public void setWfTransition(WFTransition wfTransition) {
        this.wfTransition = wfTransition;
    }

    public void cancelTransitionDetail() {
        this.wfTransition = new WFTransition();
        selectedRules.clear();
        showDetailPage = false;
    }

    @Override
    @ActionMethod
    public String saveOrUpdate(boolean killConversation) throws BusinessException {
        super.saveOrUpdate(killConversation);
        return "workflowDetail";
    }

    public void saveWfTransition() throws BusinessException{

        List<WFDecisionRule> wfDecisionRules = new ArrayList<>();

        boolean isUniqueNameValue = checkAndPopulateDecisionRules(selectedRules, wfDecisionRules);
        if (!isUniqueNameValue) {
            return;
        }

        for (WFDecisionRule wfDecisionRuleFor : wfDecisionRules) {
            if (wfDecisionRuleFor.getId() == null) {
                wfDecisionRuleService.create(wfDecisionRuleFor, getCurrentUser());
            }
        }

        if (wfTransition.getId() != null) {
            WFTransition wfTrs = wFTransitionService.findById(wfTransition.getId());
            wfTrs.setFromStatus(wfTransition.getFromStatus());
            wfTrs.setToStatus(wfTransition.getToStatus());
            wfTrs.setConditionEl(wfTransition.getConditionEl());
            wfTrs.setDescription(wfTransition.getDescription());

            wfTrs.getWfDecisionRules().clear();
            wfTrs.getWfDecisionRules().addAll(wfDecisionRules);

            wFTransitionService.update(wfTrs, getCurrentUser());

            addOrUpdateOrDeleteActions(wfTrs, wfActions, true);

            messages.info(new BundleKey("messages", "update.successful"));
        } else {
            wfTransition.getWfDecisionRules().clear();
            wfTransition.getWfDecisionRules().addAll(wfDecisionRules);

            wfTransition.setWorkflow(entity);
            wFTransitionService.create(wfTransition, getCurrentUser());

            addOrUpdateOrDeleteActions(wfTransition, wfActions, false);

            entity.getTransitions().add(wfTransition);
            messages.info(new BundleKey("messages", "save.successful"));
        }

        wfDecisionRulesByName.clear();
        selectedRules.clear();
        wfActions.clear();
        showDetailPage = false;
        wfTransition = new WFTransition();
    }

    public void deleteWfTransition(WFTransition transitionToDelete) {
        WFTransition transition = wFTransitionService.findById(transitionToDelete.getId()); 
        wFTransitionService.remove(transition);
        entity.getTransitions().remove(transitionToDelete);
        wfDecisionRulesByName.clear();
        selectedRules.clear();
        wfActions.clear();
        showDetailPage = false;
        messages.info(new BundleKey("messages", "delete.successful"));
    }

    public void editWfTransition(WFTransition transitionToEdit) {
        this.wfTransition = transitionToEdit;
        WFTransition wfTransition1 = wFTransitionService.findById(this.wfTransition.getId(), Arrays.asList("provider", "wfDecisionRules", "wfActions"), true);
        if (wfTransition1 != null && wfTransition1.getWfDecisionRules() != null) {
            wfDecisionRulesByName.clear();
            selectedRules.clear();
            for (WFDecisionRule wfDecisionRule : wfTransition1.getWfDecisionRules()) {
                GroupedDecisionRule groupedDecisionRule = new GroupedDecisionRule();
                groupedDecisionRule.setName(wfDecisionRule.getName());
                groupedDecisionRule.setValue(wfDecisionRule);
                List<WFDecisionRule> list = wfDecisionRuleService.getWFDecisionRules(wfDecisionRule.getName(), entity.getProvider());
                Collections.sort(list);
                wfDecisionRulesByName.add(list);
                selectedRules.add(groupedDecisionRule);
            }
        }
        if (wfTransition1 != null && wfTransition1.getWfActions() != null) {
            wfActions.clear();
            wfActions.addAll(wfTransition1.getWfActions());
        }
        showDetailPage = true;
    }
    
    /**
     * Autocomplete method for class filter field - search entity type classes with @ObservableEntity annotation
     * 
     * @param query A partial class name (including a package)
     * @return A list of classnames
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<String> autocompleteClassNames(String query) {
    	List<Class<?>> allWFType = workflowService.getAllWFTypes(getCurrentProvider());
    	 List<String> classNames = new ArrayList<String>();
    	for(Class<?> clazz :allWFType ){    		
    			classNames.add(clazz.getName());    		
    	}    	
        Collections.sort(classNames);
        return classNames;
    }

    /**
     * @see org.meveo.admin.action.BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<Workflow> getPersistenceService() {
        return workflowService;
    }

    @Override
    protected List<String> getFormFieldsToFetch() {
        return Arrays.asList("provider", "transitions");
    }

    @Override
    protected List<String> getListFieldsToFetch() {
        return Arrays.asList("provider");
    }
    
    @SuppressWarnings({ "unchecked" })
	public Map<String, String> getTransitionStatusFromWorkflowType() {
    	try {
			Class<?> clazz = workflowService.getWFTypeClassForName(entity.getWfType(),getCurrentProvider());    		
			Object obj = clazz.newInstance();
			Method testMethod = obj.getClass().getMethod("getStatusList");
			List<String> statusList = (List<String>) testMethod.invoke(obj);
			Map<String, String> statusMap = new TreeMap<>();
			for(String s : statusList) {
				statusMap.put(s, s);
			}
			return statusMap;
		} catch (ClassNotFoundException e) {
			log.error("unable to get class " + entity.getWfType(), e);
		} catch (InstantiationException e) {
			log.error("unable to instantiate class " + entity.getWfType(), e);
		} catch (IllegalAccessException e) {
			log.error("can not access constructor of class " + entity.getWfType(), e);
		} catch (NoSuchMethodException e) {
			log.error("unable to find getStatusList method on class " + entity.getWfType(), e);
		} catch (SecurityException e) {
			log.error(e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			log.error("illegal arguments for getStatusList method on class " + entity.getWfType(), e);
		} catch (InvocationTargetException e) {
			log.error(e.getMessage(), e);
		}
    	return new TreeMap<>();
    }

    public List<String> getWfTransitionRulesName() {
        if (wfTransitionRulesName == null) {
            wfTransitionRulesName = wfDecisionRuleService.getDistinctNameWFTransitionRules(getCurrentProvider());
        }
        return wfTransitionRulesName;
    }

    public void setWfTransitionRulesName(List<String> wfTransitionRulesName) {
        this.wfTransitionRulesName = wfTransitionRulesName;
    }

    public List<List<WFDecisionRule>> getWfDecisionRulesByName() {
        return wfDecisionRulesByName;
    }

    public void setWfDecisionRulesByName(List<List<WFDecisionRule>> wfDecisionRulesByName) {
        this.wfDecisionRulesByName = wfDecisionRulesByName;
    }

    public void changedRuleName(int indexRule) {
        List<WFDecisionRule> list = wfDecisionRuleService.getWFDecisionRules(selectedRules.get(indexRule).getName(), entity.getProvider());
        Collections.sort(list);
        if (wfDecisionRulesByName.size() > indexRule && wfDecisionRulesByName.get(indexRule) != null) {
            wfDecisionRulesByName.remove(indexRule);
            wfDecisionRulesByName.add(indexRule, list);
        } else {
            wfDecisionRulesByName.add(indexRule, list);
        }
    }

    public void addNewRule() {
        selectedRules.add(new GroupedDecisionRule());
    }

    public void addNewAction() {
        WFAction newInstance = new WFAction();
        if (CollectionUtils.isNotEmpty(wfActions)) {
            WFAction lastAction = wfActions.get(wfActions.size() - 1);
            newInstance.setPriority(lastAction.getPriority() + 1);
        } else {
            newInstance.setPriority(1);
        }
        wfActions.add(newInstance);
    }

    public void deleteWfDecisionRule(int indexRule) {
        if (wfDecisionRulesByName.size() > indexRule && wfDecisionRulesByName.get(indexRule) != null) {
            wfDecisionRulesByName.remove(indexRule);
        }
        selectedRules.remove(indexRule);
    }

    public void deleteWfAction(int indexAction) {
        if (wfActions.size() > indexAction && wfActions.get(indexAction) != null) {
            wfActions.remove(indexAction);
        }
    }

    public List<GroupedDecisionRule> getSelectedRules() {
        return selectedRules;
    }

    public void setSelectedRules(List<GroupedDecisionRule> selectedRules) {
        this.selectedRules = selectedRules;
    }

    public WFDecisionRule getNewWFDecisionRule() {
        return newWFDecisionRule;
    }

    public void setNewWFDecisionRule(WFDecisionRule newWFDecisionRule) {
        this.newWFDecisionRule = newWFDecisionRule;
    }

    public boolean isShowDetailPage() {
        return showDetailPage;
    }

    public void setShowDetailPage(boolean showDetailPage) {
        this.showDetailPage = showDetailPage;
    }

    public void newTransition() {
        showDetailPage = true;
        selectedRules.clear();
        wfActions.clear();
        wfDecisionRulesByName.clear();
        List<WFTransition> wfTransitionList = entity.getTransitions();
        if (CollectionUtils.isNotEmpty(wfTransitionList)) {
            WFTransition lastWFTransition = wfTransitionList.get(wfTransitionList.size() - 1);
            wfTransition.setPriority(lastWFTransition.getPriority() + 1);
        } else {
            wfTransition.setPriority(1);
        }
    }

    public List<WFAction> getWfActions() {
        return wfActions;
    }

    public void setWfActions(List<WFAction> wfActions) {
        this.wfActions = wfActions;
    }

    private void addOrUpdateOrDeleteActions(WFTransition wfTransition, List<WFAction> wfActionList, boolean isUpdate) throws BusinessException {

        List<WFAction> updatedActions = new ArrayList<>();
        List<WFAction> newActions = new ArrayList<>();
        for (WFAction wfAction : wfActionList) {
            if (wfAction.getId() != null) {
                updatedActions.add(wfAction);
            } else {
                newActions.add(wfAction);
            }
        }

        if (isUpdate) {
            WFTransition currentTransition = wFTransitionService.findById(this.wfTransition.getId(), Arrays.asList("wfActions"));
            List<WFAction> deletedActions = currentTransition.getWfActions();
            if (CollectionUtils.isNotEmpty(deletedActions)) {
                deletedActions.removeAll(updatedActions);
            }
            for (WFAction wfAction : deletedActions) {
                wfActionService.remove(wfAction);
            }
            for (WFAction wfAction : updatedActions) {
                wfActionService.update(wfAction, getCurrentUser());
            }
        }
        for (WFAction wfAction : newActions) {
            wfAction.setWfTransition(wfTransition);
            wfActionService.create(wfAction, getCurrentUser());
        }

        updatedActions.clear();
        newActions.clear();
    }

    public boolean checkAndPopulateDecisionRules(List<GroupedDecisionRule> groupedDecisionRules, List<WFDecisionRule> wfDecisionRules) {
        ParamBean paramBean = ParamBean.getInstance();
        String datePattern = paramBean.getProperty("meveo.dateFormat", "dd/MM/yyyy");
        List<RuleNameValue> uniqueNameValues = new ArrayList<>();
        for (GroupedDecisionRule groupedDecisionRule : groupedDecisionRules) {
            if (groupedDecisionRule.getValue() != null && groupedDecisionRule.getValue().getModel()) {
                WFDecisionRule wfDecisionRule = groupedDecisionRule.getValue();
                newWFDecisionRule.setModel(Boolean.FALSE);
                newWFDecisionRule.setConditionEl(wfDecisionRule.getConditionEl());
                newWFDecisionRule.setName(wfDecisionRule.getName());
                newWFDecisionRule.setType(wfDecisionRule.getType());
                newWFDecisionRule.setProvider(getCurrentProvider());
                newWFDecisionRule.setDisabled(Boolean.FALSE);

                if (wfDecisionRule.getType().toString().startsWith("RANGE")) {
                    StringBuffer value = new StringBuffer();
                    if (wfDecisionRule.getType() == DecisionRuleTypeEnum.RANGE_DATE) {
                        if (groupedDecisionRule.getNewDate() != null && groupedDecisionRule.getAnotherDate() == null) {
                            value.append(DateUtils.formatDateWithPattern(groupedDecisionRule.getNewDate(), datePattern)).append(LESS_SEPARATOR_NO_SPACE_RIGHT);
                        } else if (groupedDecisionRule.getNewDate() == null && groupedDecisionRule.getAnotherDate() != null) {
                            value.append(LESS_SEPARATOR_NO_SPACE_LEFT).append(DateUtils.formatDateWithPattern(groupedDecisionRule.getAnotherDate(), datePattern));
                        } else {
                            value.append(DateUtils.formatDateWithPattern(groupedDecisionRule.getNewDate(), datePattern))
                                 .append(LESS_SEPARATOR).append(DateUtils.formatDateWithPattern(groupedDecisionRule.getAnotherDate(), datePattern));
                        }
                    } else {
                        if (groupedDecisionRule.getNewValue() != null && groupedDecisionRule.getAnotherValue() == null) {
                            value.append(groupedDecisionRule.getNewValue()).append(LESS_SEPARATOR_NO_SPACE_RIGHT);
                        } else if (groupedDecisionRule.getNewValue() == null && groupedDecisionRule.getAnotherValue() != null) {
                            value.append(LESS_SEPARATOR_NO_SPACE_LEFT).append(groupedDecisionRule.getAnotherValue());
                        } else {
                            value.append(groupedDecisionRule.getNewValue())
                                 .append(LESS_SEPARATOR).append(groupedDecisionRule.getAnotherValue());
                        }
                    }
                    newWFDecisionRule.setValue(value.toString());
                } else if (wfDecisionRule.getType() == DecisionRuleTypeEnum.DATE && groupedDecisionRule.getNewDate() != null) {
                    newWFDecisionRule.setValue(DateUtils.formatDateWithPattern(groupedDecisionRule.getNewDate(), datePattern));
                } else {
                    newWFDecisionRule.setValue(groupedDecisionRule.getNewValue());
                }
                WFDecisionRule existedDecisionRule = wfDecisionRuleService.getWFDecisionRuleByNameValue(newWFDecisionRule.getName(),
                        newWFDecisionRule.getValue(), getCurrentProvider());

                if (existedDecisionRule != null) {
                    messages.error(new BundleKey("messages", "decisionRule.uniqueNameValue"), new Object[]{newWFDecisionRule.getName(), newWFDecisionRule.getValue()});
                    FacesContext.getCurrentInstance().validationFailed();
                    return false;
                }
                RuleNameValue ruleNameValue = new RuleNameValue(newWFDecisionRule.getName(), newWFDecisionRule.getValue());
                if (uniqueNameValues.contains(ruleNameValue)) {
                    messages.error(new BundleKey("messages", "decisionRule.duplicateNameValue"), new Object[]{ruleNameValue.getName(), ruleNameValue.getValue()});
                    FacesContext.getCurrentInstance().validationFailed();
                    return false;
                }

                uniqueNameValues.add(ruleNameValue);
                wfDecisionRules.add(newWFDecisionRule);
                newWFDecisionRule = new WFDecisionRule();
            } else if (groupedDecisionRule.getValue() != null) {
                RuleNameValue ruleNameValue = new RuleNameValue(groupedDecisionRule.getValue().getName(), groupedDecisionRule.getValue().getValue());
                if (uniqueNameValues.contains(ruleNameValue)) {
                    messages.error(new BundleKey("messages", "decisionRule.duplicateNameValue"), new Object[]{ruleNameValue.getName(), ruleNameValue.getValue()});
                    FacesContext.getCurrentInstance().validationFailed();
                    return false;
                }
                uniqueNameValues.add(ruleNameValue);
                wfDecisionRules.add(groupedDecisionRule.getValue());
            }
        }
        return true;
    }

    public void moveUpTransition(WFTransition selectedWfTransition) throws BusinessException {
        cancelTransitionDetail();
        int index = entity.getTransitions().indexOf(selectedWfTransition);
        if (index > 0) {
            WFTransition upWfTransition = entity.getTransitions().get(index);
            int priorityUp = upWfTransition.getPriority();
            WFTransition downWfTransition = entity.getTransitions().get(index - 1);
            WFTransition needUpdate = wFTransitionService.refreshOrRetrieve(upWfTransition);
            needUpdate.setPriority(downWfTransition.getPriority());
            wFTransitionService.update(needUpdate, getCurrentUser());
            needUpdate = wFTransitionService.refreshOrRetrieve(downWfTransition);
            needUpdate.setPriority(priorityUp);
            wFTransitionService.update(needUpdate, getCurrentUser());
            entity.getTransitions().get(index).setPriority(downWfTransition.getPriority());
            entity.getTransitions().get(index - 1).setPriority(priorityUp);
            Collections.swap(entity.getTransitions(), index, index - 1);
            messages.info(new BundleKey("messages", "update.successful"));
        }
    }

    public void moveDownTransition(WFTransition selectedWfTransition) throws BusinessException {
        cancelTransitionDetail();
        int index = entity.getTransitions().indexOf(selectedWfTransition);
        if (index < entity.getTransitions().size() - 1) {
            WFTransition upWfTransition = entity.getTransitions().get(index);
            int priorityUp = upWfTransition.getPriority();
            WFTransition downWfTransition = entity.getTransitions().get(index + 1);
            WFTransition needUpdate = wFTransitionService.findById(upWfTransition.getId(), true);
            needUpdate.setPriority(downWfTransition.getPriority());
            wFTransitionService.update(needUpdate, getCurrentUser());
            needUpdate = wFTransitionService.findById(downWfTransition.getId(), true);
            needUpdate.setPriority(priorityUp);
            wFTransitionService.update(needUpdate, getCurrentUser());
            entity.getTransitions().get(index).setPriority(downWfTransition.getPriority());
            entity.getTransitions().get(index + 1).setPriority(priorityUp);
            Collections.swap(entity.getTransitions(), index, index + 1);
            messages.info(new BundleKey("messages", "update.successful"));
        }
    }

    public void moveUpAction(int index) {
        if (index > 0) {
            WFAction upWfAction = wfActions.get(index);
            int priorityUp = upWfAction.getPriority();
            WFAction downWfAction = wfActions.get(index - 1);
            wfActions.get(index).setPriority(downWfAction.getPriority());
            wfActions.get(index - 1).setPriority(priorityUp);
            Collections.swap(wfActions, index, index - 1);
        }
    }

    public void moveDownAction(int index) {
        if (index < wfActions.size() - 1) {
            WFAction upWfAction = wfActions.get(index);
            int priorityUp = upWfAction.getPriority();
            WFAction downWfAction = wfActions.get(index + 1);
            wfActions.get(index).setPriority(downWfAction.getPriority());
            wfActions.get(index + 1).setPriority(priorityUp);
            Collections.swap(wfActions, index, index + 1);
        }
    }

    public class RuleNameValue implements Serializable {

        private static final long serialVersionUID = 3694377290046737073L;
        private String name;
        private String value;

        public RuleNameValue() {
        }

        public RuleNameValue(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RuleNameValue nameValue = (RuleNameValue) o;

            if (!name.equals(nameValue.name)) return false;
            if (!value.equals(nameValue.value)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }
    }
}