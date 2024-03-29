package com.ssafysignal.api.apply.service;

import com.ssafysignal.api.admin.Repository.BlackUserRepository;
import com.ssafysignal.api.apply.dto.request.BasicApplyRequest;
import com.ssafysignal.api.apply.dto.request.ApplyMemoRequest;
import com.ssafysignal.api.apply.dto.response.FindApplyResponse;
import com.ssafysignal.api.apply.dto.response.FindApplyWriterResponse;
import com.ssafysignal.api.apply.entity.*;
import com.ssafysignal.api.apply.repository.*;
import com.ssafysignal.api.common.entity.CommonCode;
import com.ssafysignal.api.common.repository.CommonCodeRepository;
import com.ssafysignal.api.common.service.SecurityService;
import com.ssafysignal.api.global.exception.NotFoundException;
import com.ssafysignal.api.global.response.ResponseCode;
import com.ssafysignal.api.posting.entity.PostingMeeting;
import com.ssafysignal.api.posting.entity.PostingPosition;
import com.ssafysignal.api.posting.repository.PostingMeetingRepository;
import com.ssafysignal.api.posting.repository.PostingPositionRepository;
import com.ssafysignal.api.project.entity.Project;
import com.ssafysignal.api.project.repository.ProjectRepository;
import com.ssafysignal.api.user.entity.User;
import com.ssafysignal.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static java.util.stream.Collectors.reducing;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplyService {
    private final ApplyRepository applyRepository;
    private final ApplyCareerRepository applyCareerRepository;
    private final ApplyExpRepository applyExpRepository;
    private final ApplySkillRepository applySkillRepository;
    private final ApplyAnswerRepository applyAnswerRepository;
    private final PostingMeetingRepository postingMeetingRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PostingPositionRepository postingPositionRepository;
    private final UserRepository userRepository;
    private final BlackUserRepository blackUserRepository;
    private final ProjectRepository projectRepository;
    private final SecurityService securityService;

    @Transactional
    public void registApply(BasicApplyRequest applyRegistRequest, Integer postingSeq) throws RuntimeException {

        User user = userRepository.findByUserSeq(applyRegistRequest.getUserSeq()).orElseThrow(
                () -> new NotFoundException(ResponseCode.REGIST_NOT_FOUNT));
        Project project = projectRepository.findByPostingSeq(postingSeq).orElseThrow(
                () -> new NotFoundException(ResponseCode.REGIST_NOT_FOUNT));

        if (blackUserRepository.findByUserSeqAndProjectSeq(user.getUserSeq(), project.getProjectSeq()).isPresent()) {
            throw new NotFoundException(ResponseCode.REGIST_BLACK);
        }

        if (applyRepository.findTop1ByUserSeqAndPostingSeq(user.getUserSeq(), postingSeq).isPresent()) {
            Apply apply = applyRepository.findTop1ByUserSeqAndPostingSeq(user.getUserSeq(), postingSeq).get();
            // 1. 이전 지원서가 아니고 지원취소한 지원서가 아니면
            // 2. 거절한 지원서이면
            if ((!(apply.getApplyCode().equals("AS109") && apply.getStateCode().equals("PAS109")) && !(apply.getApplyCode().equals("AS104") && apply.getStateCode().equals("PAS104")))||
                apply.getApplyCode().equals("AS102") && apply.getStateCode().equals("PAS104")) {
                throw new NotFoundException(ResponseCode.REGIST_DUPLICATE);
                // 재지원 불가능
            }
        }

        int userHeartCnt = user.getHeartCnt();
        if (userHeartCnt < 100) {
            throw new NotFoundException(ResponseCode.REGIST_LACK_HEART);
        }

        Apply apply = Apply.builder()
                .userSeq(applyRegistRequest.getUserSeq())
                .postingSeq(postingSeq)
                .content(applyRegistRequest.getContent())
                .positionCode(applyRegistRequest.getPositionCode())
                .postingMeetingSeq(applyRegistRequest.getPostingMeetingSeq())
                .build();
        applyRepository.save(apply);

        for (String skill : applyRegistRequest.getApplySkillList()) {
            applySkillRepository.save(ApplySkill.builder()
                    .applySeq(apply.getApplySeq())
                    .skillCode(skill)
                    .build()
            );
        }

        for (String exp : applyRegistRequest.getApplyExpList()) {
            applyExpRepository.save(ApplyExp.builder()
                    .applySeq(apply.getApplySeq())
                    .content(exp)
                    .build()
            );
        }

        for (String career : applyRegistRequest.getApplyCareerList()) {
            applyCareerRepository.save(ApplyCareer.builder()
                    .applySeq(apply.getApplySeq())
                    .content(career)
                    .build()
            );
        }

        for(Map<String, Object> answerRequest : applyRegistRequest.getApplyAnswerList()){
            Integer postingQuestionSeq = (Integer) answerRequest.get("postingQuestionSeq");
            String content = (String)answerRequest.get("content");
            ApplyAnswer applyAnswer = ApplyAnswer.builder()
                    .applySeq(apply.getApplySeq())
                    .postingSeq(postingSeq)
                    .postingQuestionSeq(postingQuestionSeq)
                    .content(content)
                    .build();
            applyAnswerRepository.save(applyAnswer);
        }

        PostingMeeting postingMeeting= postingMeetingRepository.findById(applyRegistRequest.getPostingMeetingSeq()).get();

        if(!postingMeeting.getCode().getCode().equals("PM102")){
            throw new DuplicateKeyException("이미 선택된 사전미팅시간");
        }
        postingMeeting.setToUserSeq(applyRegistRequest.getUserSeq());
        postingMeeting.setApplySeq(apply.getApplySeq());
        postingMeeting.setPostingMeetingCode("PM100");

        postingMeetingRepository.save(postingMeeting);
    }
    @Transactional(readOnly = true)
    public FindApplyResponse findApply(Integer applySeq) {
        Apply apply = applyRepository.findById(applySeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));
        String positionCode = apply.getPositionCode();
        CommonCode code = commonCodeRepository.findById(positionCode).get();

        List<ApplySkill> skillList = applySkillRepository.findAllByApplySeq(applySeq);
        List<CommonCode> skillCommonCodeList = new ArrayList<>();
        for (ApplySkill as : skillList){
            String skillCode = as.getSkillCode();
            CommonCode skillCommonCode = commonCodeRepository.findById(skillCode).get();
            skillCommonCodeList.add(skillCommonCode);
        }

        PostingMeeting postingMeeting = postingMeetingRepository.findByApplySeqAndToUserSeq(applySeq, apply.getUserSeq());
        Integer userSeq = securityService.currentUserSeq();

        return FindApplyResponse.builder()
                .userSeq(apply.getUserSeq())
                .postingSeq(apply.getPostingSeq())
                .content(apply.getContent())
                .position(code)
                .answerList(apply.getApplyAnswerList())
                .careerList(apply.getApplyCareerList())
                .expList(apply.getApplyExpList())
                .skillList(skillCommonCodeList)
                .postingMeeting(postingMeeting)
                .isMyApply(apply.getUserSeq().equals(userSeq))
                .build();
    }
    @Transactional
    public void modifyApply(BasicApplyRequest applyModifyRequest, Integer applySeq) throws RuntimeException {
        Apply apply = applyRepository.findById(applySeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        apply.setContent(applyModifyRequest.getContent());
        apply.setPositionCode(applyModifyRequest.getPositionCode());

        List<ApplySkill> applySkillList = apply.getApplySkillList();
        applySkillList.clear();
        for (String skill : applyModifyRequest.getApplySkillList()) {
            applySkillList.add(ApplySkill.builder()
                    .applySeq(applySeq)
                    .skillCode(skill)
                    .build()
            );
        }
        apply.setApplySkillList(applySkillList);

        List<ApplyExp> applyExpList = apply.getApplyExpList();
        applyExpList.clear();
        for (String exp : applyModifyRequest.getApplyExpList()) {
            applyExpList.add(ApplyExp.builder()
                    .applySeq(applySeq)
                    .content(exp)
                    .build()
            );
        }
        apply.setApplyExpList(applyExpList);

        List<ApplyCareer> applyCareerList = apply.getApplyCareerList();
        applyCareerList.clear();
        for (String career : applyModifyRequest.getApplyCareerList()) {
            applyCareerList.add(ApplyCareer.builder()
                    .applySeq(applySeq)
                    .content(career)
                    .build()
            );
        }
        apply.setApplyCareerList(applyCareerList);

        for(Map<String, Object> answerRequest : applyModifyRequest.getApplyAnswerList()){
            Integer applyAnswerSeq = (Integer) answerRequest.get("applyAnswerSeq");
            String content = (String)answerRequest.get("content");
            ApplyAnswer answer = applyAnswerRepository.findById(applyAnswerSeq).get();
            answer.setContent(content);
            applyAnswerRepository.save(answer);
        }

        Integer newPostingMeetingSeq = applyModifyRequest.getPostingMeetingSeq();
        Integer curPostingMeetingSeq = apply.getPostingMeetingSeq();
        if(!newPostingMeetingSeq.equals(curPostingMeetingSeq)) {
        	PostingMeeting newPostingmeeting = postingMeetingRepository.findById(newPostingMeetingSeq).get();
        	if(!newPostingmeeting.getPostingMeetingCode().equals("PM102")) 
        		throw new DuplicateKeyException("이미 선택된 사전미팅시간");
        	apply.setPostingMeetingSeq(newPostingMeetingSeq);
        	
        	PostingMeeting curPostingMeeting = postingMeetingRepository.findById(curPostingMeetingSeq).get();
        	if(curPostingMeeting.getPostingMeetingCode().equals("PM101"))
        		throw new DuplicateKeyException("이미 사전미팅 했음");
        	curPostingMeeting.setApplySeq(null);
        	curPostingMeeting.setToUserSeq(null);
        	curPostingMeeting.setPostingMeetingCode("PM102");
        	postingMeetingRepository.save(curPostingMeeting);
        	
        	newPostingmeeting.setApplySeq(applySeq);
        	newPostingmeeting.setToUserSeq(applyModifyRequest.getUserSeq());
        	newPostingmeeting.setPostingMeetingCode("PM100");
        	postingMeetingRepository.save(newPostingmeeting);
        }
        applyRepository.save(apply);
    }
    @Transactional
    public void cancleApply(Integer applySeq) throws RuntimeException {
        Apply apply = applyRepository.findById(applySeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        Integer postingMeetingSeq = apply.getPostingMeetingSeq();
        PostingMeeting postingMeeting= postingMeetingRepository.findById(postingMeetingSeq).get();
        if (!postingMeeting.getPostingMeetingCode().equals("PM101")) {
        	postingMeeting.setApplySeq(null);
        	postingMeeting.setToUserSeq(null);
        	postingMeeting.setPostingMeetingCode("PM102");
        	postingMeetingRepository.save(postingMeeting);
        }
        apply.setApplyCode("AS104");
        apply.setStateCode("PAS104");
        applyRepository.save(apply);
    }
    @Transactional(readOnly = true)
    public String findApplyMemo(Integer applySeq) {
        Apply apply = applyRepository.findById(applySeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        return apply.getMemo();
    }
    @Transactional(readOnly = true)
    public Map<String,Integer> countApplyWriter(Integer postingSeq){
        Integer totalCnt = applyRepository.countByPostingSeq(postingSeq);
        return new HashMap<String, Integer>() {{ put("count", totalCnt); }};
    }
    @Transactional(readOnly = true)
    public Map<String,Integer> countApplyApplyer(Integer userSeq){
        Integer totalCnt = applyRepository.countByUserSeq(userSeq);
        return new HashMap<String, Integer>() {{ put("count", totalCnt); }};
    }
    @Transactional(readOnly = true)
    public Map<String, Object> findAllApplyWriter(Integer postingSeq, Integer page, Integer size){
        List<Apply> applyList = applyRepository.findAllByPostingSeq(postingSeq, PageRequest.of(page - 1, size, Sort.Direction.DESC, "applySeq"));
        Integer totalCnt = postingPositionRepository.findPostingPositiosnByPostingSeq(postingSeq).stream().map(PostingPosition::getPositionCnt).collect(reducing(Integer::sum)).get();
        Integer selectCnt = applyRepository.countByPostingSeqAndApplyCode(postingSeq, "AS101");
        Integer waitCnt = applyRepository.countByPostingSeqAndApplyCode(postingSeq, "AS100");

        Map<String, Object> resList = new HashMap<>();
        resList.put("applyList", FindApplyWriterResponse.toList(applyList));
        resList.put("totalCnt", totalCnt);
        resList.put("selectCnt",selectCnt);
        resList.put("waitCnt", waitCnt);
        return resList;
    }
    @Transactional(readOnly = true)
    public List<Apply> findAllApplyApplyer(Integer userSeq, Integer page, Integer size){
        List<Apply> applyList = applyRepository.findAllByUserSeq(userSeq, PageRequest.of(page - 1, size, Sort.Direction.DESC, "applySeq"));
        return applyList;
    }
    @Transactional
    public void modifyApplyMemo(ApplyMemoRequest applyMemoRequest) {
        Apply apply = applyRepository.findById(applyMemoRequest.getApplySeq())
                        .orElseThrow(() -> new NotFoundException(ResponseCode.MODIFY_NOT_FOUND));
        apply.setMemo(applyMemoRequest.getMemo());
        applyRepository.save(apply);
    }
}