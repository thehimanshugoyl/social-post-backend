package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.model.DlqMessage;
import java.util.List;
import java.util.Optional;

public interface DlqManagementService {

    DlqMessage recordDlqMessage(DlqMessage message);

    List<DlqMessage> getAllDlqMessages();

    List<DlqMessage> getUnresolvedDlqMessages();

    Optional<DlqMessage> getDlqMessageById(Long id);

    DlqMessage markResolved(Long id, String resolutionNotes);

    void replayDlqMessage(Long id);
}
