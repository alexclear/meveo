package org.meveo.api.job;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseApi;
import org.meveo.api.dto.job.TimerEntityDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.crm.Provider;
import org.meveo.model.jobs.TimerEntity;
import org.meveo.service.job.TimerEntityService;

@Stateless
public class TimerEntityApi extends BaseApi {

    @Inject
    private TimerEntityService timerEntityService;

    public void create(TimerEntityDto timerEntityDto, User currentUser) throws MeveoApiException, BusinessException {
        if (StringUtils.isBlank(timerEntityDto.getCode()) || StringUtils.isBlank(timerEntityDto.getHour()) || StringUtils.isBlank(timerEntityDto.getMinute())
                || StringUtils.isBlank(timerEntityDto.getSecond()) || StringUtils.isBlank(timerEntityDto.getYear()) || StringUtils.isBlank(timerEntityDto.getMonth())
                || StringUtils.isBlank(timerEntityDto.getDayOfMonth()) || StringUtils.isBlank(timerEntityDto.getDayOfWeek())) {

            if (StringUtils.isBlank(timerEntityDto.getHour())) {
                missingParameters.add("hour");
            }
            if (StringUtils.isBlank(timerEntityDto.getMinute())) {
                missingParameters.add("minute");
            }
            if (StringUtils.isBlank(timerEntityDto.getSecond())) {
                missingParameters.add("second");
            }
            if (StringUtils.isBlank(timerEntityDto.getYear())) {
                missingParameters.add("year");
            }
            if (StringUtils.isBlank(timerEntityDto.getMonth())) {
                missingParameters.add("month");
            }
            if (StringUtils.isBlank(timerEntityDto.getDayOfMonth())) {
                missingParameters.add("dayOfMonth");
            }
            if (StringUtils.isBlank(timerEntityDto.getDayOfWeek())) {
                missingParameters.add("dayOfWeek");
            }

            handleMissingParameters();

        }

        Provider provider = currentUser.getProvider();

        if (timerEntityService.findByCode(timerEntityDto.getCode(), provider) != null) {
            throw new EntityAlreadyExistsException(TimerEntity.class, timerEntityDto.getCode());
        }

        TimerEntity timerEntity = TimerEntityDto.fromDTO(timerEntityDto, null);

        timerEntityService.create(timerEntity, currentUser);
    }

    public void update(TimerEntityDto timerEntityDto, User currentUser) throws MeveoApiException, BusinessException {

        String timerEntityCode = timerEntityDto.getCode();
        Provider provider = currentUser.getProvider();

        if (StringUtils.isBlank(timerEntityCode)) {
            missingParameters.add("Code");
            handleMissingParameters();
        }

        TimerEntity timerEntity = timerEntityService.findByCode(timerEntityCode, provider);
        if (timerEntity == null) {
            throw new EntityDoesNotExistsException(TimerEntity.class, timerEntityCode);
        }

        timerEntity = TimerEntityDto.fromDTO(timerEntityDto, timerEntity);

        timerEntityService.update(timerEntity, currentUser);
    }

    public void createOrUpdate(TimerEntityDto timerEntityDto, User currentUser) throws MeveoApiException, BusinessException {

        if (timerEntityService.findByCode(timerEntityDto.getCode(), currentUser.getProvider()) == null) {
            create(timerEntityDto, currentUser);
        } else {
            update(timerEntityDto, currentUser);
        }
    }

    public TimerEntityDto find(String timerEntityCode, User currentUser) throws MeveoApiException {
        TimerEntityDto result = new TimerEntityDto();
        if (StringUtils.isBlank(timerEntityCode)) {
            missingParameters.add("code");
            handleMissingParameters();
        }
        TimerEntity timerEntity = timerEntityService.findByCode(timerEntityCode, currentUser.getProvider());
        if (timerEntity == null) {
            throw new EntityDoesNotExistsException(timerEntityCode.getClass(), timerEntityCode);
        }
        result = new TimerEntityDto(timerEntity);

        return result;
    }

    public void remove(String timerEntityCode, User currentUser) throws MeveoApiException {
        if (StringUtils.isBlank(timerEntityCode)) {
            missingParameters.add("code");
            handleMissingParameters();
        }
        TimerEntity timerEntity = timerEntityService.findByCode(timerEntityCode, currentUser.getProvider());
        if (timerEntity == null) {
            throw new EntityDoesNotExistsException(timerEntityCode.getClass(), timerEntityCode);
        }

        timerEntityService.remove(timerEntity);
    }
}