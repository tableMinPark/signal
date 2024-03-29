package com.ssafysignal.api.project.service;

import com.ssafysignal.api.admin.Entity.BlackUser;
import com.ssafysignal.api.admin.Repository.BlackUserRepository;
import com.ssafysignal.api.common.entity.ImageFile;
import com.ssafysignal.api.common.repository.ImageFileRepository;
import com.ssafysignal.api.common.service.FileService;
import com.ssafysignal.api.common.service.SecurityService;
import com.ssafysignal.api.global.exception.NotFoundException;
import com.ssafysignal.api.global.response.ResponseCode;
import com.ssafysignal.api.letter.entity.Letter;
import com.ssafysignal.api.letter.repository.LetterRepository;
import com.ssafysignal.api.project.dto.reponse.FindEvaluationResponse;
import com.ssafysignal.api.project.dto.reponse.FindProjectSettingResponse;
import com.ssafysignal.api.project.dto.reponse.FindAllProjectUserDto;
import com.ssafysignal.api.project.dto.reponse.FindProjectUserEvaluationHistoryResponse;
import com.ssafysignal.api.project.dto.request.RegistProjectEvaluationRequest;
import com.ssafysignal.api.project.dto.request.ModifyProjectSettingRequest;
import com.ssafysignal.api.project.entity.*;
import com.ssafysignal.api.project.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectSettingService {

    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final ProjectEvaluationQuestionRepository projectEvaluationQuestionRepository;
    private final ProjectPositionRepository projectPositionRepository;
    private final BlackUserRepository blackUserRepository;
    private final ImageFileRepository imageFileRepository;
    private final FileService fileService;
    private final SecurityService securityService;
    private final LetterRepository letterRepository;

    @Value("${app.fileUpload.uploadPath}")
    private String uploadPath;

    @Value("${app.fileUpload.uploadPath.projectImage}")
    private String projectUploadPath;
    @Transactional(readOnly = true)
    public FindProjectSettingResponse findProjectSetting(Integer projectSeq) {
        Project project = projectRepository.findById(projectSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        return FindProjectSettingResponse.fromEntity(project);
    }

    @Transactional(readOnly = true)
    public List<FindAllProjectUserDto> findProjectUser(Integer projectSeq) {
        Integer userSeq = securityService.currentUserSeq();

        List<ProjectUser> projectUserList = projectUserRepository.findByProjectSeq(projectSeq);

        // 씹억까 Integer wrapper 객체를 비교할 때는 equals 사용 (별표 100개)
        for (int idx = 0; idx < projectUserList.size(); idx++){
            if (projectUserList.get(idx).getUserSeq().equals(userSeq)){
                projectUserList.add(0, projectUserList.get(idx));
                projectUserList.remove(idx + 1);
            }
        }
        return projectUserList.stream()
                .map(FindAllProjectUserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Integer> findProjectUserEvaluation(Integer projectUserSeq, Integer weekCnt) {
        projectUserRepository.findById(projectUserSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        List<ProjectEvaluation> projectEvaluationList = projectEvaluationRepository.findAll(ProjectSpecification.byFromUserSeq(projectUserSeq, weekCnt));
        return projectEvaluationList.stream()
                .map(ProjectEvaluation::getToUserSeq)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void modifyProjectSetting(Integer projectSeq, MultipartFile uploadImage, ModifyProjectSettingRequest modifyProjectSettingRequest) throws RuntimeException, IOException {
        Project project = projectRepository.findById(projectSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.MODIFY_NOT_FOUND));

        if (uploadImage != null){
            // 사진올리고
            ImageFile imageFile = fileService.registImageFile(uploadImage, projectUploadPath);
            if (project.getProjectImageFileSeq() != 1) {
                fileService.deleteImageFile(uploadPath + project.getImageFile().getUrl());
                project.getImageFile().setType(imageFile.getType());
                project.getImageFile().setUrl(imageFile.getUrl());
                project.getImageFile().setName(imageFile.getName());
                project.getImageFile().setSize(imageFile.getSize());
                project.getImageFile().setRegDt(LocalDateTime.now());
            } else {
                ImageFile newImageFile = ImageFile.builder()
                        .name(imageFile.getName())
                        .size(imageFile.getSize())
                        .url(imageFile.getUrl())
                        .type(imageFile.getType())
                        .build();
                imageFileRepository.save(newImageFile);
                project.setProjectImageFileSeq(newImageFile.getImageFileSeq());
            }
        }

        if (modifyProjectSettingRequest.getIsDelete()){
            fileService.deleteImageFile(uploadPath + project.getImageFile().getUrl());
            project.setProjectImageFileSeq(1);
        }
        /*
            프로젝트 설정 데이터 처리
         */
        project.setSubject(modifyProjectSettingRequest.getSubject());
        project.setLocalCode(modifyProjectSettingRequest.getLocalCode());
        project.setFieldCode(modifyProjectSettingRequest.getFieldCode());
        project.setTerm(modifyProjectSettingRequest.getTerm());
        project.setContact(modifyProjectSettingRequest.isContact());
        project.setContent(modifyProjectSettingRequest.getContent());
        project.setGitUrl(modifyProjectSettingRequest.getGitUrl());
        projectRepository.save(project);
    }

    @Transactional
    public void deleteProjectUser(Integer projectUserSeq) throws RuntimeException {
        ProjectUser projectUser = projectUserRepository.findById(projectUserSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.DELETE_NOT_FOUND));

        Project project = projectRepository.findById(projectUser.getProjectSeq())
                .orElseThrow(() -> new NotFoundException(ResponseCode.DELETE_NOT_FOUND));

        letterRepository.save(Letter.builder()
                    .fromUserSeq(0)
                    .toUserSeq(projectUser.getUserSeq())
                    .title("프로젝트 퇴출 안내")
                    .content("<div>경고 "+ projectUser.getWarningCnt()  + " 번으로 팀장에 의해 " +  project.getSubject() + " 프로젝트에서 퇴출 되었습니다.</div><br> <div>보증금 " + projectUser.getHeartCnt() + "개의 하트는 소멸되었음을 알려드립니다.</div><br>")
                    .build());

        // 블랙리스트 등록
        blackUserRepository.save(BlackUser.builder()
                        .userSeq(projectUser.getUserSeq())
                        .projectSeq(projectUser.getProjectSeq())
                        .build());

        // 현재 프로젝트 인원에서 제거
        projectUserRepository.deleteById(projectUserSeq);
        
        // 포지션 인원 맞춤 (제거된 인원 포지션 -1)
        ProjectPosition projectPosition = projectPositionRepository.findByProjectSeqAndPositionCode(projectUser.getProjectSeq(), projectUser.getPositionCode())
                .orElseThrow(() -> new NotFoundException(ResponseCode.DELETE_NOT_FOUND));
        projectPosition.setPositionCnt(projectPosition.getPositionCnt() - 1);
        projectPositionRepository.save(projectPosition);

    }

    @Transactional
    public void registProjectUserEvaluation(RegistProjectEvaluationRequest registProjectEvaluationRequest) throws RuntimeException {
        // 이미 등록됬는지 확인
        if (projectEvaluationRepository.findAll(
                ProjectSpecification.byFromUserSeqAndToUserSeq(
                        registProjectEvaluationRequest.getFromUserSeq(),
                        registProjectEvaluationRequest.getToUserSeq(),
                        registProjectEvaluationRequest.getWeekCnt())).isEmpty()){

            for (Map<String, Integer> score : registProjectEvaluationRequest.getScoreList()){

                projectEvaluationRepository.save(ProjectEvaluation.builder()
                        .projectSeq(registProjectEvaluationRequest.getProjectSeq())
                        .fromUserSeq(registProjectEvaluationRequest.getFromUserSeq())
                        .toUserSeq(registProjectEvaluationRequest.getToUserSeq())
                        .weekCnt(registProjectEvaluationRequest.getWeekCnt())
                        .num(score.get("num"))
                        .score(score.get("score"))
                        .build());
            }
        } else throw new NotFoundException(ResponseCode.REGIST_ALREADY);
    }

    @Transactional(readOnly = true)
    public FindEvaluationResponse findAllEvalution(Integer projectSeq) {
        Project project = projectRepository.findById(projectSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));

        List<ProjectEvaluationQuestion> projectEvaluationQuestionList = projectEvaluationQuestionRepository.findAll();

        return FindEvaluationResponse.fromEntity(project, projectEvaluationQuestionList);
    }

    @Transactional(readOnly = true)
    public Integer countWeekCnt(Integer projectSeq) {
        Project project = projectRepository.findById(projectSeq)
                .orElseThrow(() -> new NotFoundException(ResponseCode.NOT_FOUND));
        return project.getWeekCnt();
    }

    @Transactional(readOnly = true)
    public List<FindProjectUserEvaluationHistoryResponse> findProjectUserEvaluationHistory(Integer fromUserSeq, Integer toUserSeq, Integer weekCnt) {
        List<ProjectEvaluation> projectEvaluationList = projectEvaluationRepository.findByFromUserSeqAndToUserSeqAndWeekCnt(fromUserSeq, toUserSeq, weekCnt);
        return FindProjectUserEvaluationHistoryResponse.toList(projectEvaluationList);
    }
}
