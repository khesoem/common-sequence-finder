package ir.pegahtech.tapsell.core.services.controllers;

import ir.pegahtech.tapsell.core.data.dao.*;
import ir.pegahtech.tapsell.core.data.entities.*;
import ir.pegahtech.tapsell.core.data.mongo.MobileUserRequestEntity;
import ir.pegahtech.tapsell.core.data.mongo.SuggestedCallToActionEntity;
import ir.pegahtech.tapsell.core.services.models.*;
import ir.pegahtech.tapsell.core.services.models.appservices.*;
import ir.pegahtech.tapsell.core.services.services.CachingServices;
import ir.pegahtech.tapsell.core.services.services.CallToActionServices;
import ir.pegahtech.tapsell.core.services.services.EffectivenessCalculatorServices;
import ir.pegahtech.tapsell.core.services.services.MobileUserRequestService;
import ir.pegahtech.tapsell.core.services.services.models.cahcing.*;
//import ir.pegahtech.tapsell.core.services.utils.GAHelper;
import ir.pegahtech.tapsell.core.services.utils.TmpDataManager;
import ir.pegahtech.tapsell.history.client.PurchaseHistoryProxy;
import ir.pegahtech.tapsell.history.client.RequestHistoryProxy;
import ir.pegahtech.tapsell.history.client.models.purchasehistory.RegisterPurchaseRequest;
import ir.pegahtech.tapsell.history.client.models.requesthistory.*;
import ir.pegahtech.utility.date.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: khashayar
 * Date: 12/16/14
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequestMapping(value = "/appservice/")
public class AppServicesController {
    public static final String CHECK_USER_INFO = "1";
    public static final String CHECK_CTA_AVAILABILITY = "2";
    public static final String CHECK_IS_DONE = "3";
    public static final String GET_DIRECT = "4";
    public static final String GET_SINGLE_SUGGESTION = "5";

    public static final String VIDEO_SDK = "video-sdk";
    public static final String OLD_SDK = "old-sdk";

    public final static int VideoPlay_TYPE_SKIPPABLE = 31;

    public static final Integer VERSION_2_1_0_CODE = 20100; // 2 * 100 * 100 + 1 * 100 + 0
    public final static int VideoPlay_TYPE_NON_SKIPPABLE = 32;

    public final static String WebView_Like_Instagram = "1";
    public final static String WebView_Follow_Instagram = "2";
    public final static String WebView_General = "3";
    public final static String WebView_Pole = "4";
    public final static String WebView_Follow_Telegram = "5";
    public final static String WebView_Direct_Ask_Server = "6";
    public final static String WebView_Complete_General = "7";

    private static Integer TEST_MINIMUM_AWARD = -2;
    private static Integer userCallToActionListSize = 15;

    private CallToActionServices callToActionServices;
//    @Autowired
//    ServletContext servletContext;

    private AwardCodeDao awardCodeDao;
    private MobileUserDao mobileUserDao;
    private CallToActionDao callToActionDao;
    private DeveloperAppDao developerAppDao;
    private VideoPlayActionDao videoPlayActionDao;
    private DeveloperAppProductDao developerAppProductDao;
    private PurchaseDao purchaseDao;
    private DeveloperAppCallToActionAccessDao developerAppCallToActionAccessDao;
    private PPCActionDao ppcActionDao;
    private PPIActionDao ppiActionDao;
    @Autowired
    private RequestHistoryProxy requestHistoryProxy;
    @Autowired
    private PurchaseHistoryProxy purchaseHistoryProxy;

    @Autowired
    MobileUserRequestService mobileUserRequestService;

    @Autowired
    EffectivenessCalculatorServices effectivenessService;

    @Autowired
    CachingServices cachingServices;

    private Map<String, Long> requestCountMapping = new HashMap<String, Long>();
    private Long allRequestsCount = 0L;

    private static Logger logger = LoggerFactory.getLogger(SuggestionsController.class);


    @RequestMapping
            (
                    value = "user/{userId}/developer_apps/{developerAppId}/purchase/{sku}/consume",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Void> consume
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable("developerAppId")
                    UUID developerAppId,
                    @PathVariable(value = "sku")
                    String sku
            ) {
        DeveloperAppProduct product = developerAppProductDao.loadBySku(developerAppId, sku);
        if (product == null) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }
        Purchase purchase = purchaseDao.purchasedBeforeAndNotConsumed(product.getGuid(), userId);
        MobileUser user = mobileUserDao.loadById(userId);
        if (purchase == null || user == null) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }

        purchase.setConsumed(true);
        purchaseDao.saveOrUpdate(purchase);

        cachingServices.evictUserPurchases(userId);

        // Finally return OK response if everything is done correct
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "user/{userId}/product/{productId}/purchase",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<UUID> purchase
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable(value = "productId")
                    UUID productId
            ) {
        DeveloperAppProduct product = developerAppProductDao.loadById(productId);
        DeveloperApp developerApp = product.getDeveloperApp();
        MobileUser user = mobileUserDao.loadById(userId);

        if (product == null || user == null) {
            return new ResponseEntity<UUID>(HttpStatus.NOT_FOUND);
        }

        if (product.getConsumable() && purchaseDao.getNotConsumedPurchase(productId, userId) != null) {
            return new ResponseEntity<UUID>(HttpStatus.BAD_REQUEST);
        }
        if (product.getPrice() > user.getTapCoin()) {
            return new ResponseEntity<UUID>(HttpStatus.BAD_REQUEST);
        }
        if (!product.getConsumable() && purchaseDao.purchasedBefore(productId, userId) != null) {
            return new ResponseEntity<UUID>(HttpStatus.BAD_REQUEST);
        }

        // Save Purchase to Core
        Purchase purchase = new Purchase();
        purchase.setProductId(productId);
        purchase.setUserId(userId);
        purchase.setConsumed(false);
        purchaseDao.saveOrUpdate(purchase);

        // Save Purchase to History
        RegisterPurchaseRequest purchaseHistory = new RegisterPurchaseRequest();
        purchaseHistory.setUserId(userId);
        purchaseHistory.setPrice(product.getPrice());
        purchaseHistory.setProductId(productId);
        purchaseHistory.setDeveloperId(developerApp.getDeveloperId());
        purchaseHistory.setDeveloperAppId(developerApp.getGuid());

        user.setTapCoin(user.getTapCoin() - product.getPrice());
        mobileUserDao.saveOrUpdate(user);

        ResponseEntity<Void> historyResponse = purchaseHistoryProxy.registerAPurchase(purchaseHistory);
        if (!historyResponse.getStatusCode().equals(HttpStatus.OK))
            return new ResponseEntity<UUID>(historyResponse.getStatusCode());


        cachingServices.evictUserPurchases(userId);
        cachingServices.evictMobileUserByImei(user.getImei());
        cachingServices.evictMobileUserById(userId);

        // Finally return OK response if everything is done correct
        return new ResponseEntity<UUID>(purchase.getGuid(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "user/{userId}/product/{productId}/consume",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Void> consumeProduct
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable(value = "productId")
                    UUID productId
            ) {
        DeveloperAppProduct product = developerAppProductDao.loadById(productId);
        MobileUser user = mobileUserDao.loadById(userId);
        if (product == null || user == null) {
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }
        if (!product.getConsumable()) {
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        Purchase purchase = purchaseDao.getNotConsumedPurchase(productId, userId);
        if (purchase == null) {
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        purchase.setConsumed(true);
        purchaseDao.saveOrUpdate(purchase);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "user/{userId}/product/{productId}/purchase/notconsumed",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Boolean> hasNotConsumedPurchase
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable(value = "productId")
                    UUID productId
            ) {
        DeveloperAppProduct product = developerAppProductDao.loadById(productId);
        MobileUser user = mobileUserDao.loadById(userId);
        if (product == null || user == null) {
            return new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND);
        }
        if (purchaseDao.getNotConsumedPurchase(productId, userId) == null) {
            return new ResponseEntity<Boolean>(false, HttpStatus.OK);
        }
        return new ResponseEntity<Boolean>(true, HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "user/{userId}/products/{developerAppId}/available",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    /*
     * Returns list of product that are not bought by user or are bought and already consumed
     */
    public ResponseEntity<AppServicesAvailableProductsResponse> hasNotConsumedPurchases
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable("developerAppId")
                    UUID developerAppId
            ) {
        CachedMobileUser user = cachingServices.loadMobileUserById(userId);
        if (user == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        List<CachedDeveloperAppProduct> availableProducts = cachingServices.getNotConsumedPurchases(userId, developerAppId);
//        List<DeveloperAppProduct> availableProducts = developerAppProductDao
//                .getNotConsumedPurchases(userId, developerAppId);
        AppServicesAvailableProductsResponse response = new AppServicesAvailableProductsResponse();
        response.setAvailableProducts(new ArrayList<AvailableProductItem>());

        for (CachedDeveloperAppProduct availableProduct : availableProducts) {
            if (availableProduct.getActiveness() != null && !availableProduct.getActiveness())
                continue;
            response.getAvailableProducts().add(new AvailableProductItem(availableProduct));
        }

        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "user/{userId}/products/{developerAppId}/purchased-not-consumed/",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    /*
     * Returns list of product that are bought and not consumed
     */
    public ResponseEntity<NotConsumedPurchaseIdsResponse> purchasedAndNotConsumed
            (
                    @PathVariable(value = "userId")
                    UUID userId,
                    @PathVariable("developerAppId")
                    UUID developerAppId
            ) {
        MobileUser user = mobileUserDao.loadById(userId);
        if (user == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        List<Purchase> availableProducts = purchaseDao
                .getPurchasedButNotConsumedYet(userId, developerAppId);

        return new ResponseEntity(new NotConsumedPurchaseIdsResponse(availableProducts), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "{mobileUserId}/tapcoin",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> getUserTapCoin
            (
                    @PathVariable(value = "mobileUserId")
                    UUID userId
            ) {
        MobileUser user = mobileUserDao.loadById(userId);
        if (user == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Integer>(user.getTapCoin(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/checkdonetype",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Boolean> getCheckDoneType
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {
        SuggestedCallToActionEntity suggestion = mobileUserRequestService.findSuggestionById(suggestionId);
        if (suggestion == null) {
            return new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND);
        }

        CallToAction callToAction = callToActionDao.loadById(suggestion.getCallToActionId());

        return new ResponseEntity<Boolean>(callToAction.checkFromServer(), HttpStatus.OK);
    }

    @RequestMapping(value = "suggestion/{mobileUserId}/", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    public ResponseEntity<AppServicesSuggestionListResponse> getSuggestions
            (
                    @PathVariable("mobileUserId")
                    UUID mobileUserId,
                    @RequestParam(value = "status")
                    Integer status,
                    @RequestParam(value = "startIndex")
                    Integer startIndex,
                    @RequestParam(value = "pageSize")
                    Integer pageSize
            ) {
        // Query list of suggestions applying input filters
        List<SuggestedCallToActionEntity> suggestions = new ArrayList<SuggestedCallToActionEntity>();
        // TODO: this should be fixed: mobileUserRequestService.getSuggestedCallToActionList
//                (
//                        mobileUserId,
//                        status,
//                        startIndex,
//                        pageSize
//                );

        // Initiate Response Object
        AppServicesSuggestionListResponse response = new AppServicesSuggestionListResponse();
        response.setItems(new ArrayList<AppServicesSuggestionListItem>());

        // Convert List of Suggestions to Response Items
        for (SuggestedCallToActionEntity suggestion : suggestions) {
            CallToAction callToAction = callToActionDao.loadById(suggestion.getCallToActionId());
            response.getItems().add(new AppServicesSuggestionListItem(suggestion, callToAction));
        }

        // Return OK response
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/done",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> setSuggestionDone
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {

        SuggestedCallToActionEntity suggestion = mobileUserRequestService.findSuggestionById(suggestionId);

        if (suggestion == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }

        CallToAction callToAction = callToActionDao.loadById(suggestion.getCallToActionId());
        if (callToAction.getType() == CallToAction.VideoPlay_TYPE) {
            VideoPlayAction videoPlayAction = videoPlayActionDao.loadById(callToAction.getGuid());
            if (videoPlayAction.getExtraUrlAndroid() != null) {
                String extraLink = videoPlayAction.getExtraUrlAndroid();
                if (extraLink.contains(VideoPlayAction.WITH_ESSENTIAL_EXTRA_ACTION)
                        || extraLink.contains(VideoPlayAction.COMPLETE_ON_CLICK)
                        || extraLink.contains(VideoPlayAction.NO_COMPLETE_ON_VIEW)) {
                    return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
                }
            }
        }

        MobileUserRequestEntity mobileUserRequest = mobileUserRequestService.findRequestBySuggestionId(suggestionId);
        MobileUser mobileUser = mobileUserDao.loadById(mobileUserRequest.getMobileUserId());

        callToActionServices.setSuggestionDone(suggestion, mobileUserRequest, mobileUser);

        return new ResponseEntity<Integer>(callToAction.getAwardTapCoin(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/done-and-got-award",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> setSuggestionDoneAndGotAward
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {

        SuggestedCallToActionEntity suggestion = mobileUserRequestService.findSuggestionById(suggestionId);

        if (suggestion == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }

        CallToAction callToAction = callToActionDao.loadById(suggestion.getCallToActionId());
        if (callToAction.getType() == CallToAction.VideoPlay_TYPE) {
            VideoPlayAction videoPlayAction = videoPlayActionDao.loadById(callToAction.getGuid());
            if (videoPlayAction.getExtraUrlAndroid() != null) {
                String extraLink = videoPlayAction.getExtraUrlAndroid();
                if (extraLink.contains(VideoPlayAction.WITH_ESSENTIAL_EXTRA_ACTION) ||
                        extraLink.contains(VideoPlayAction.NO_COMPLETE_ON_VIEW) ||
                        (extraLink.contains(VideoPlayAction.COMPLETE_ON_CLICK) &&
                                (extraLink.contains(VideoPlayAction.TELEGRAM_COMMON_LINK)
                                        || extraLink.contains(VideoPlayAction.INSTAGRAM_CLICK_COMMON_LINK)))) {
                    return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
                }
            }
        }

        MobileUserRequestEntity mobileUserRequest = mobileUserRequestService.findRequestBySuggestionId(suggestion.getId());
        MobileUser mobileUser = mobileUserDao.loadById(mobileUserRequest.getMobileUserId());

        callToActionServices.setSuggestionDoneAndGotAward(suggestion, mobileUserRequest, mobileUser, true);

        return new ResponseEntity<Integer>(callToAction.getAwardTapCoin(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/mini-game-done",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> setSuggestionDoneFromMiniGame
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {

        SuggestedCallToActionEntity suggestion = mobileUserRequestService.findSuggestionById(suggestionId);

        if (suggestion == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }

        MobileUserRequestEntity mobileUserRequest = mobileUserRequestService.findRequestBySuggestionId(suggestionId);
        MobileUser mobileUser = mobileUserDao.loadById(mobileUserRequest.getMobileUserId());
        CallToAction callToAction = callToActionDao.loadById(suggestion.getCallToActionId());

        callToActionServices.setSuggestionDone(suggestion, mobileUserRequest, mobileUser);

        return new ResponseEntity<Integer>(callToAction.getAwardTapCoin(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/doing",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> setSuggestionDoing
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {
        MobileUserRequestEntity mobileUserRequest = mobileUserRequestService.findRequestBySuggestionId(suggestionId);
        if (mobileUserRequest == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }

        // FIXME: too many queries for a simple task
        UUID developerAppId = mobileUserRequest.getDeveloperAppId();
        UUID developerId = cachingServices
                .loadDeveloperAppById(developerAppId)
                .getDeveloperId();

        SuggestedCallToActionEntity doingSuggestion = null;
        for (SuggestedCallToActionEntity suggestion : mobileUserRequest.getSuggestions()) {
            if (suggestion.getId().equals(suggestionId))
                doingSuggestion = suggestion;
        }

        // Creating History request
        ResponseEntity<Void> historyResponse = createHistoryRecord(doingSuggestion, mobileUserRequest,
                mobileUserRequest.getSuggestions(), developerId);
        if (!historyResponse.getStatusCode().equals(HttpStatus.OK))
            return new ResponseEntity<Integer>(historyResponse.getStatusCode());

        mobileUserRequestService.changeStateAndClearOtherSuggestions(mobileUserRequest, doingSuggestion, SuggestedCallToActionEntity.DOING);
        return new ResponseEntity<Integer>(0, HttpStatus.OK);
    }

    protected ResponseEntity<Void> createHistoryRecord
            (
                    SuggestedCallToActionEntity doingSuggestion,
                    MobileUserRequestEntity mobileUserRequest,
                    List<SuggestedCallToActionEntity> allSuggestions,
                    UUID developerId
            ) {
        RequestHistoryRegisterRequest historyRequest = new RequestHistoryRegisterRequest();
        historyRequest.setCallToActionId(doingSuggestion == null ? null : doingSuggestion.getCallToActionId());
        historyRequest.setCreationDate(mobileUserRequest.getCreationDate());
        historyRequest.setDeveloperAppId(mobileUserRequest.getDeveloperAppId());
        historyRequest.setMobileUserId(mobileUserRequest.getMobileUserId());
        historyRequest.setRequestId(mobileUserRequest.getRequestId());
        historyRequest.setState(mobileUserRequest.getState());
        historyRequest.setDeveloperId(developerId);

        historyRequest.setSuggestedCallToActions(new ArrayList<SuggestedCallToActionInfo>());

        for (SuggestedCallToActionEntity suggestedCallToAction : allSuggestions) {
//            if (doingSuggestion != null && suggestedCallToAction.getGuid().equals(doingSuggestion.getGuid()))
//                continue;

            SuggestedCallToActionInfo info = new SuggestedCallToActionInfo();
            info.setCallToActionId(suggestedCallToAction.getCallToActionId());
            info.setMobileUserRequestId(mobileUserRequest.getRequestId());
            info.setState(suggestedCallToAction.getState());
            info.setSuggestedAward(suggestedCallToAction.getSuggestedAward());
            info.setSuggestedCallToActionId(suggestedCallToAction.getId());
            info.setEventDate(new Timestamp(mobileUserRequest.getCreationDate().getTime()));

            if (doingSuggestion != null && suggestedCallToAction.getId().equals(doingSuggestion.getId())) {
                info.setEventDate(new Timestamp(new Date().getTime()));
                info.setState(SuggestedCallToActionEntity.DOING);
            }

//            if (info.getState().equals(SuggestedCallToAction.DOING))
//                info.setEventDate(new Timestamp(new Date().getTime()));

            historyRequest.getSuggestedCallToActions().add(info);
        }

        ResponseEntity<Void> historyResponse = requestHistoryProxy.registerRequest(historyRequest);
        return historyResponse;
    }

    @RequestMapping
            (
                    value = "suggestion/{suggestionId}/status",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<Integer> checkSuggestionState
            (
                    @PathVariable(value = "suggestionId")
                    UUID suggestionId
            ) {
        SuggestedCallToActionEntity suggestion = mobileUserRequestService.findSuggestionById(suggestionId);
        if (suggestion == null)
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<Integer>(suggestion.getState(), HttpStatus.OK);
    }

//    @RequestMapping
//            (
//                    value = "suggestion/{suggestionId}/isdone",
//                    method = RequestMethod.GET,
//                    produces = "application/json; charset=utf-8"
//            )
//    public ResponseEntity<Boolean> checkIsDone
//            (
//                    @PathVariable(value = "suggestionId")
//                    UUID suggestionId
//            ){
//        SuggestedCallToAction suggestion = suggestedCallToActionDao.loadById(suggestionId);
//        if(suggestion == null){
//            return new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND);
//        }
//        if(suggestion.getState() == SuggestedCallToAction.DONE){
//            return new ResponseEntity<Boolean>(true, HttpStatus.OK);
//        }
//        if(suggestion.getCallToAction().checkIsDone(suggestion.getMobileUserRequest().getMobileUser())){
//            suggestion.setState(SuggestedCallToAction.DONE);
//            suggestedCallToActionDao.saveOrUpdate(suggestion);
//            suggestion.getMobileUserRequest().setState(MobileUserRequest.DONE);
//            mobileUserRequestDao.saveOrUpdate(suggestion.getMobileUserRequest());
//            callToActionServices.setSuggestionDone(suggestion);
//            return new ResponseEntity<Boolean>(true, HttpStatus.OK);
//        }else{
//            return new ResponseEntity<Boolean>(false, HttpStatus.OK);
//        }
//    }

    @RequestMapping
            (
                    value = "awardcodes/new",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<String> newAward
            (
                    @RequestBody
                    NewAwardRequest request
            ) {
        AwardCode awardCode = awardCodeDao.generateCode(request.getAmount(), request.getAppId());
        return new ResponseEntity<String>(awardCode.getKey(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "awardcodes/{awardcode}/consumeby/{mobileUserId}",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity consumeAwardCode
            (
                    @PathVariable("awardcode")
                    String awardCode,
                    @PathVariable("mobileUserId")
                    UUID mobileUserId
            ) {
        AwardCode award = awardCodeDao.loadByKey(awardCode);
        if (award == null || !award.getState().equals(AwardCode.STATE_CREATED))
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        MobileUser mobileUser = mobileUserDao.loadById(mobileUserId);
        if (mobileUser == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        if (award.getDeveloperAppId() != null &&
                (award.getDeveloperApp().getApplicationInfo().getAndroidPackageName() == null ||
                        !mobileUser.getImei().contains(award.getDeveloperApp().getApplicationInfo().getAndroidPackageName())) &&
                (award.getDeveloperApp().getApplicationInfo().getIosPackageName1() == null ||
                        !mobileUser.getImei().contains(award.getDeveloperApp().getApplicationInfo().getIosPackageName1())) &&
                (award.getDeveloperApp().getApplicationInfo().getIosPackageName2() == null ||
                        !mobileUser.getImei().contains(award.getDeveloperApp().getApplicationInfo().getIosPackageName2()))) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        mobileUser.setTapCoin(mobileUser.getTapCoin() + award.getTapCoinAward());
        award.setState(AwardCode.STATE_USED);
        award.setUsedByUserId(mobileUser.getGuid());

        mobileUserDao.saveOrUpdate(mobileUser);
        awardCodeDao.saveOrUpdate(award);
        cachingServices.evictMobileUserByImei(mobileUser.getImei());
        cachingServices.evictMobileUserById(mobileUserId);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "awardcodes/awardamount",
                    method = RequestMethod.GET,
                    produces = "application/json; charset=utf-8"

            )
    public ResponseEntity<Integer> getAwardAmount
            (
                    @RequestParam("key")
                    String key
            ) {
        AwardCode awardCode = awardCodeDao.loadByKey(key);
        if (awardCode == null) {
            return new ResponseEntity<Integer>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Integer>(awardCode.getTapCoinAward(), HttpStatus.OK);
    }

    @RequestMapping
            (
                    value = "calltoaction/",
                    method = RequestMethod.POST,
                    produces = "application/json; charset=utf-8"
            )
    public ResponseEntity<CallToActionListResponse> getCallToActionList
            (
                    @RequestBody
                    CallToActionListRequest request
            ) {
        if (request.getSdkType() == null)
            request.setSdkType(OLD_SDK);
        long l1 = new Date().getTime();
        CachedDeveloperApp app = cachingServices.loadDeveloperAppById(request.getAppId());
        CachedApplicationInfo appInfo = app.getApplicationInfo();

        String correctPackageName = null;
        if (request.getTargetPlatform() == CallToAction.TARGET_ANDROID)
            correctPackageName = appInfo.getAndroidPackageName();
        else if (request.getTargetPlatform() == CallToAction.TARGET_IOS)
            correctPackageName = appInfo.getIosPackageName1();

        if (app == null || correctPackageName == null || !correctPackageName.equals(request.getAppPackageName())) {
            return new ResponseEntity<CallToActionListResponse>(HttpStatus.NOT_FOUND);
        }
        CachedMobileUser mobileUser = cachingServices.loadMobileUserById(request.getMobileUserId());
        if (mobileUser == null)
            return new ResponseEntity<CallToActionListResponse>(HttpStatus.NOT_FOUND);

        logger.info("time t1 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        // Check if user has requested more than his daily quota
        long now = new Date().getTime();
        long todayTime = DateUtility.getDayOfDate(new Date(now)).getTime();
        ResponseEntity<CachedUserAppDayDoneStat> todayStatResponse = cachingServices.
                getDoneStatOfUser(request.getMobileUserId(), app.getGuid(), todayTime);

        if (!todayStatResponse.getStatusCode().equals(HttpStatus.OK))
            return new ResponseEntity<CallToActionListResponse>(todayStatResponse.getStatusCode());

        long sumOfTodayAwards = todayStatResponse.getBody().getDoneAward();
        Integer appMaxAward = app.getMaxEarningOfUserPerDay();
        if (appMaxAward != null && sumOfTodayAwards >= appMaxAward) {
            CallToActionListResponse response =
                    new CallToActionListResponse(new HashMap<UUID, UUID>(), new ArrayList<CachedCallToAction>(), true);
            return new ResponseEntity<CallToActionListResponse>(response, HttpStatus.OK);
        }

        long countOfTodayDone = todayStatResponse.getBody().getDoneCount();
        Integer appMaxDoneCount = app.getMaxCountOfDoneCtaPerDay();
        if (appMaxDoneCount != null && countOfTodayDone >= appMaxDoneCount) {
            CallToActionListResponse response =
                    new CallToActionListResponse(new HashMap<UUID, UUID>(), new ArrayList<CachedCallToAction>(), true);
            return new ResponseEntity<CallToActionListResponse>(response, HttpStatus.OK);
        }
        // end of daily limit check

        logger.info("time t2 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        ResponseEntity<CachedDoneCallToActionsResponse> callToActionsResponse = cachingServices
                .getListOfDoneCallToActions(mobileUser.getGuid());

        if (!callToActionsResponse.getStatusCode().equals(HttpStatus.OK))
            return new ResponseEntity<CallToActionListResponse>(callToActionsResponse.getStatusCode());

        // set minimumAward to support test mode //
        if (request.getMinimumAward() == TEST_MINIMUM_AWARD) {
            request.setMinimumAward(0);
        } else {
            if (request.getMinimumAward() != null) {
                if (!checkCanIgnoreMinimumAward(app)) {
                    request.setMinimumAward(Math.max(request.getMinimumAward(), 2));
                } else {
                    request.setMinimumAward(2);
                }
            } else {
                request.setMinimumAward(2);
            }
        }
        // end of setting minimumAward to support test mode //

        Integer dbType = request.getType();
        // check for skippable-video type
        if (dbType != null) {
            if (dbType == VideoPlay_TYPE_NON_SKIPPABLE || dbType == VideoPlay_TYPE_SKIPPABLE) {
                dbType = CallToAction.VideoPlay_TYPE;
            }
        }
        // end of check for skippable-video type

        // getting accessible ctas
        List<UUID> accessibleCtaIdsByCid =
                request.getGmsCid() == null || request.getGmsCid() <= 0
                        ? new ArrayList<UUID>()
                        : cachingServices.loadAccessibleCtaByCid(
                        request.getGmsCid(),
                        request.getGmsLac(),
                        request.getMcc(),
                        request.getMnc());

        List<UUID> accessibleCtaIdsByIp =
                request.getIp() == null ? new ArrayList<UUID>()
                        : cachingServices.loadAccessibleCtaByIp(request.getIp());

        List<UUID> accessibleCtaIdsByLocation = new ArrayList<UUID>();
        accessibleCtaIdsByLocation.addAll(accessibleCtaIdsByCid);
        if (accessibleCtaIdsByIp != null)
            accessibleCtaIdsByLocation.addAll(accessibleCtaIdsByIp);

        List<UUID> genreSupporters =
                appInfo.getGenreId() == null
                        ? new ArrayList<UUID>()
                        : cachingServices.loadAccessibleCtasByGenre(appInfo.getGenreId());

        List<UUID> ageRestrictionSupporters =
                appInfo.getAgeRestrictionId() == null
                        ? new ArrayList<UUID>()
                        : cachingServices.loadAccessibleCtasByAgeRestriction(appInfo.getAgeRestrictionId());

        List<CachedCallToAction> allCallToActions =
                cachingServices.listBestCallToActions(
                        app.getGuid(),
                        request.getMinimumAward(),
                        dbType,
                        accessibleCtaIdsByLocation,
                        genreSupporters,
                        ageRestrictionSupporters,
                        new ExternalAgentUserInfo
                                (
                                        request.getIp(),
                                        request.getMobileUserId().toString(),
                                        request.getAppPackageName()
                                )
                );

        List<CachedCallToAction> resultCallToAction = new ArrayList<CachedCallToAction>();

        logger.info("time t3 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        List<CachedDoneCallToActionsItems> doneCallToActions = callToActionsResponse.getBody().getDoneCallToActionIds();
        List<CachedCtaAppStat> ctaAppStats =
                cachingServices.loadCachedCtaAppStatListByAppId(app.getGuid()).getAppStats();

        double donePerMonth = getDonePerMonth(doneCallToActions.size(), mobileUser.getCreationDate());

        for (int i = 0; i < allCallToActions.size(); i++) {
            CachedCallToAction callToAction = allCallToActions.get(i);
            long doneCount = getNumberOfDone(doneCallToActions, callToAction.getGuid());
            long lastDoneTime = getLastDoneTime(doneCallToActions, callToAction.getGuid());

            if (hasConflictingAd(callToAction, doneCallToActions))
                continue;

            if (doneCount >= callToAction.getMaxRepeatPerUserTimes())
                continue;

            if (now - lastDoneTime < callToAction.getMillisecondsBetweenRepeats())
                continue;

            if (!callToAction.supportsPlatform(request.getTargetPlatform()))
                continue;

            if (!checkCtaTypeAndRequestedTypeCompatibility(request.getType(), callToAction)) {
                continue;
            }

            if (!isCtaCompatibleWithSDK(callToAction, request.getSdkVersion(), request.getSdkType())) {
                continue;
            }

            if (isCtaDisabledOnApp(callToAction, ctaAppStats)) {
                continue;
            }

            if (isHardCoreUser(callToAction, donePerMonth, doneCallToActions.size())) {
                continue;
            }

            if (isCtaSubtypeDisabledOnApp(callToAction, app)) {
                continue;
            }

            if (isLimitedForUser(callToAction, mobileUser)) {
                continue;
            }

            resultCallToAction.add(callToAction);
            if (resultCallToAction.size() >= userCallToActionListSize)
                break;
        }

        logger.info("time t4 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        logger.info("time t9 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        // Register new request
        MobileUserRequestEntity userRequest = new MobileUserRequestEntity();
        userRequest.setMobileUserId(mobileUser.getGuid());
        userRequest.setDeveloperAppId(app.getGuid());
        userRequest.setRequestId(UUID.randomUUID());
        userRequest.setCreationDate(new Date());
        userRequest.setLastModifiedDate(userRequest.getCreationDate());
        userRequest.setIsDeleted(false);
        userRequest.setState(MobileUserRequestEntity.SUGGESTED_TO_USER);
        userRequest.setCallToActionId(null);
        if (request.getAppUserInfo() != null && request.getAppUserInfo().checkContainsValue()) {
            userRequest.setAppUserInfo(request.getAppUserInfo().extractEntity());
        }


        Map<UUID, UUID> mappedCallToActions = new HashMap<UUID, UUID>(); // map from callToActionId -> suggestionId
        List<SuggestedCallToActionEntity> newSuggestions = new ArrayList<SuggestedCallToActionEntity>();
        for (CachedCallToAction cta : resultCallToAction) {
            SuggestedCallToActionEntity suggestion = new SuggestedCallToActionEntity();
            suggestion.setState(SuggestedCallToActionEntity.JUST_SUGGESTED);
            suggestion.setSuggestedAward(cta.getAwardTapCoin());
            suggestion.setCallToActionId(cta.getGuid());
            suggestion.setExternalAdId(cta.getExternalAgentAdId() == null ? null : cta.getExternalAgentAdId());
            suggestion.setExternalAgentDoneCallbackUri(cta.getExternalAgentDoneCallbackUri() == null ? null
                    : cta.calculateExternalAgentDoneCallbackUri(request)
            );
            suggestion.setId(UUID.randomUUID());

            if (cta.getEffectivenessLimitsCnt() != null && cta.getEffectivenessLimitsCnt() != 0) { // should involve effectiveness
                Integer award = getCtaAwardOnApp(ctaAppStats, cta);
                if (award != null) {
                    suggestion.setSuggestedAward(award);
                }
            }

            newSuggestions.add(suggestion);

            if (request.getRequestType() != null && request.getRequestType().equals(GET_SINGLE_SUGGESTION)) // other suggestions are not needed
                break;
        }

        userRequest.setSuggestions(newSuggestions);

        long l2 = new Date().getTime();
        if (isRequestWithValidSuggestions(request.getRequestType())) {
            mobileUserRequestService.save(userRequest);
        }
        logger.info("time t11 is: " + (new Date().getTime() - l2));

        for (int i = 0; i < resultCallToAction.size() && i < newSuggestions.size(); i++) {
            CachedCallToAction cta = resultCallToAction.get(i);
            SuggestedCallToActionEntity suggestion = newSuggestions.get(i);

            // if award is changed because of effectiveness
            cta.setAwardTapCoin(suggestion.getSuggestedAward());

            mappedCallToActions.put(cta.getGuid(), suggestion.getId());
        }

        logger.info("time t10 is: " + (new Date().getTime() - l1));
        l1 = new Date().getTime();

        return new ResponseEntity(new CallToActionListResponse(mappedCallToActions, resultCallToAction, false), HttpStatus.OK);
    }

    private boolean isHardCoreUser(CachedCallToAction callToAction, double donePerMonth, int totalDone) {
        if (callToAction.getGuid().equals(UUID.fromString("89f40f63-470b-4cb0-92d9-82f791875535")) && totalDone > 25) {
            return true;
        }

        return false;
    }

    private double getDonePerMonth(int doneCount, long creationDate) {
        long duration = new Date().getTime() - creationDate;

        return doneCount * 30.0 * 24 * 60 * 60 / duration;
    }

    private boolean isLimitedForUser(CachedCallToAction callToAction, CachedMobileUser mobileUser) {
        if (callToAction.getGuid().equals(UUID.fromString("00c1b17b-5070-44e4-8741-4662e70d4afe"))) {
            String device = mobileUser.getDeviceModelName();
            if(device == null){
                return true;
            }
            for(String preferredDevice : TmpDataManager.getInstance().getDragDuelPreferredDevices()){
                if(device.contains(preferredDevice)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isCtaSubtypeDisabledOnApp(CachedCallToAction callToAction, CachedDeveloperApp app) {
        Integer disability = app.getCtaSubtypesDisability();
        if (disability == null)
            return false;
        int ind = 0;
        Integer ctaSubtype = callToAction.calculateSubtype();
        while (disability > 0) {
            if (ctaSubtype.equals(ind)) {
                int b = disability % 2;
                if (b == 1) {
                    return true;
                }
                break;
            }
            disability = disability / 2;
            ind++;
        }
        return false;
    }

    private boolean checkCanIgnoreMinimumAward(CachedDeveloperApp app) {
        if (app.getDeveloperId().equals(UUID.fromString("182315f6-3331-4236-80df-b6bddbafc325"))) {
            return false;
        }
        return true;
    }

    private Integer getCtaAwardOnApp(List<CachedCtaAppStat> ctaAppStats, CachedCallToAction cta) {
        for (CachedCtaAppStat stat : ctaAppStats) {
            if (cta.getGuid().equals(stat.getCtaId())) {
                return stat.getAward();
            }
        }
        return null;
    }

    private boolean isCtaDisabledOnApp(CachedCallToAction callToAction, List<CachedCtaAppStat> stats) {
        if (callToAction.getEffectivenessLimitsCnt() == null || callToAction.getEffectivenessLimitsCnt() == 0) {
            return false;
        }
        for (CachedCtaAppStat stat : stats) {
            if (stat.getCtaId().equals(callToAction.getGuid())) {
                if (stat.getActive() != null) {
                    return !stat.getActive();
                }
                break;
            }
        }
        return false;
    }

    private boolean checkCtaTypeAndRequestedTypeCompatibility(Integer requestedType, CachedCallToAction cta) {
        if (requestedType != null && requestedType == VideoPlay_TYPE_NON_SKIPPABLE) {
            boolean isSkippable = cta.hasSkipPoint();
            if (isSkippable) {
                return false;
            }
        }
        if (requestedType != null && requestedType == VideoPlay_TYPE_SKIPPABLE) {
            boolean isSkippable = cta.hasSkipPoint();
            if (!isSkippable) {
                return false;
            }
        }
        return true;
    }

    private boolean getLastDoneTime(List<CachedDoneCallToActionsItems> doneCallToActions) {
        return false;
    }

    public static Integer getSdkCodeFromVersionName(String sdkVersion) {
        String[] versionParts = sdkVersion.split("\\.");
        Integer versionCode = Integer.parseInt(versionParts[0]) * 10000;
        if (versionParts.length > 1) {
            versionCode += Integer.parseInt(versionParts[1]) * 100;
        }
        if (versionParts.length > 2) {
            versionCode += Integer.parseInt(versionParts[2]);
        }
        return versionCode;
    }

    private boolean isCtaCompatibleWithSDK(CachedCallToAction cta, String sdkVersion, String sdkType) {
        if (("1.0".equals(sdkVersion) || "1.1".equals(sdkVersion)) && cta.getType() == CallToAction.WebView_TYPE) {
            return false;
        }

        Integer sdkVersionCode = getSdkCodeFromVersionName(sdkVersion);

        // Cannot open intent on < 2.1.0
        if (cta.getType() == CallToAction.VideoPlay_TYPE) {
            if (cta.getVideoPlayAction() != null
                    && cta.getVideoPlayAction().getExtraUrlAndroid() != null
                    && cta.getVideoPlayAction().getExtraUrlAndroid().contains("{{show_web_view}}")) {
                if (sdkVersion != null && sdkVersionCode < VERSION_2_1_0_CODE)
                    return false;
            }
        }

        String typeInfo = cta.getCalculatedIdentifierInfo();
        if ((typeInfo.contains("Instagram-Follow:::") || typeInfo.contains("Telegram-Follow:::")
                || typeInfo.contains("CheckPackageExists:::"))
                && sdkVersionCode < VERSION_2_1_0_CODE) {
            // it has got videoActionCompletionStrategy and sdk does not support it!
            return false;
        }
        if (cta.getType() == CallToAction.WebView_TYPE) {
            String[] parts = typeInfo.split(":::#:::");
            if (parts.length > 0) {
                String webViewType = parts[0];
                if (webViewType.equals(WebView_Complete_General)) {
                    if (parts.length > 2) {
                        String link = parts[2];
                        if (link.contains("advertiserId")
                                && sdkVersionCode < VERSION_2_1_0_CODE) {
                            // it's redirect-mod (Ex. clickyab) and sdk does not support it!
                            return false;
                        }
                    }
                }
            }
        }
        if (cta.hasSkipPoint()) {
            if (!sdkType.equals(VIDEO_SDK)) {
                return false;
            }
        }
        return true;
    }

    @RequestMapping(value = "/developer_app_cached/{secretKey}/", method = RequestMethod.GET,
            produces = "application/json; charset=utf-8")
    public ResponseEntity<DeveloperAppDetailsResponse> getDeveloperAppBySecretKeyCached
            (

                    @PathVariable("secretKey")
                    String secretKey
            ) {
        CachedDeveloperApp developerApp = cachingServices.loadDeveloperAppBySecretKey(secretKey);
        if (developerApp == null)
            return new ResponseEntity(HttpStatus.NOT_FOUND);

        DeveloperAppDetailsResponse response = new DeveloperAppDetailsResponse(developerApp);
        return new ResponseEntity(response, HttpStatus.OK);
    }


    private Long getNumberOfDone(List<CachedDoneCallToActionsItems> doneCallToActions, UUID guid) {
        for (CachedDoneCallToActionsItems item : doneCallToActions) {
            if (item.getCallToActionId().equals(guid))
                return item.getDoneCount();
        }
        return 0L;
    }

    private Long getLastDoneTime(List<CachedDoneCallToActionsItems> doneCallToActions, UUID guid) {
        for (CachedDoneCallToActionsItems item : doneCallToActions)
            if (item.getCallToActionId().equals(guid))
                return item.getLastDoneTime();
        return 0L;
    }

    private boolean hasConflictingAd(CachedCallToAction callToAction, List<CachedDoneCallToActionsItems> doneCallToActions) {
        UUID first = UUID.fromString("f7ab62b4-cb1e-43ea-8dc0-54d14c478021");
        UUID second = UUID.fromString("4519907f-f44b-4caf-8f05-cd6394c7e421");

        if (!callToAction.getGuid().equals(first) && !callToAction.getGuid().equals(second))
            return false;

        for (CachedDoneCallToActionsItems item : doneCallToActions)
            if (item.getCallToActionId().equals(first) || item.getCallToActionId().equals(second))
                return true;

        return false;
    }

    private Integer versionNameToVersionCode(String versionName) {
        String[] parts = versionName.split("/.");
        Integer versionCode = 0;
        versionCode += Integer.parseInt(parts[0]) * 1000000;
        if (parts[1] != null)
            versionCode += Integer.parseInt(parts[1]) * 10000;
        if (parts[2] != null)
            versionCode += Integer.parseInt(parts[2]) * 100;
        if (parts[3] != null)
            versionCode += Integer.parseInt(parts[3]);
        return versionCode;
    }

    private boolean isDataManipulatorRequest(String requestType) {
        return requestType == null || !(requestType.equals(CHECK_USER_INFO) || requestType.equals(CHECK_CTA_AVAILABILITY));
    }

    private boolean isRequestWithValidSuggestions(String requestType) {
        return requestType == null ||
                !(
                        requestType.equals(CHECK_USER_INFO) ||
                                requestType.equals(CHECK_CTA_AVAILABILITY) ||
                                requestType.equals(CHECK_IS_DONE)
                );
    }

    public MobileUserDao getMobileUserDao() {
        return mobileUserDao;
    }

    public void setMobileUserDao(MobileUserDao mobileUserDao) {
        this.mobileUserDao = mobileUserDao;
    }

    public CallToActionDao getCallToActionDao() {
        return callToActionDao;
    }

    public void setCallToActionDao(CallToActionDao callToActionDao) {
        this.callToActionDao = callToActionDao;
    }

    public DeveloperAppDao getDeveloperAppDao() {
        return developerAppDao;
    }

    public void setDeveloperAppDao(DeveloperAppDao developerAppDao) {
        this.developerAppDao = developerAppDao;
    }

    public void setAwardCodeDao(AwardCodeDao awardCodeDao) {
        this.awardCodeDao = awardCodeDao;
    }

    public DeveloperAppProductDao getDeveloperAppProductDao() {
        return developerAppProductDao;
    }

    public void setDeveloperAppProductDao(DeveloperAppProductDao developerAppProductDao) {
        this.developerAppProductDao = developerAppProductDao;
    }

    public void setPurchaseDao(PurchaseDao purchaseDao) {
        this.purchaseDao = purchaseDao;
    }

    public DeveloperAppCallToActionAccessDao getDeveloperAppCallToActionAccessDao() {
        return developerAppCallToActionAccessDao;
    }

    public void setDeveloperAppCallToActionAccessDao(DeveloperAppCallToActionAccessDao developerAppCallToActionAccessDao) {
        this.developerAppCallToActionAccessDao = developerAppCallToActionAccessDao;
    }

    public RequestHistoryProxy getRequestHistoryProxy() {
        return requestHistoryProxy;
    }

    public void setRequestHistoryProxy(RequestHistoryProxy requestHistoryProxy) {
        this.requestHistoryProxy = requestHistoryProxy;
    }

    public PurchaseHistoryProxy getPurchaseHistoryProxy() {
        return purchaseHistoryProxy;
    }

    public void setPurchaseHistoryProxy(PurchaseHistoryProxy purchaseHistoryProxy) {
        this.purchaseHistoryProxy = purchaseHistoryProxy;
    }

    public PPIActionDao getPpiActionDao() {
        return ppiActionDao;
    }

    public void setPpiActionDao(PPIActionDao ppiActionDao) {
        this.ppiActionDao = ppiActionDao;
    }

    public PPCActionDao getPpcActionDao() {
        return ppcActionDao;
    }

    public void setPpcActionDao(PPCActionDao ppcActionDao) {
        this.ppcActionDao = ppcActionDao;
    }

    public CallToActionServices getCallToActionServices() {
        return callToActionServices;
    }

    public void setCallToActionServices(CallToActionServices callToActionServices) {
        this.callToActionServices = callToActionServices;
    }

    public VideoPlayActionDao getVideoPlayActionDao() {
        return videoPlayActionDao;
    }

    public void setVideoPlayActionDao(VideoPlayActionDao videoPlayActionDao) {
        this.videoPlayActionDao = videoPlayActionDao;
    }
}
