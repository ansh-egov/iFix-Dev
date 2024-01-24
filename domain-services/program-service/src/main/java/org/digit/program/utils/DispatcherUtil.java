package org.digit.program.utils;

import org.digit.program.constants.Action;
import org.digit.program.constants.MessageType;
import org.digit.program.models.Program;
import org.digit.program.models.RequestJsonMessage;
import org.digit.program.models.RequestMessage;
import org.digit.program.repository.ServiceRequestRepository;
import org.springframework.stereotype.Component;

@Component
public class DispatcherUtil {

    private final ServiceRequestRepository restRepo;

    public DispatcherUtil(ServiceRequestRepository restRepo) {
        this.restRepo = restRepo;
    }

    public void sendOnProgram(RequestJsonMessage requestJsonMessage, Program program){
        RequestMessage requestMessage = RequestMessage.builder().id(requestJsonMessage.getId())
                .header(requestJsonMessage.getHeader()).signature(requestJsonMessage.getSignature())
                .message(program.toString()).build();
        updateUri(requestMessage);
        StringBuilder url = new StringBuilder(requestMessage.getHeader().getReceiverId()).append("exchange/v1/on-program");
        restRepo.fetchResult(url, requestMessage);
    }

    private void updateUri(RequestMessage requestMessage){
        String senderId = requestMessage.getHeader().getSenderId();
        requestMessage.getHeader().setSenderId(requestMessage.getHeader().getReceiverId());
        requestMessage.getHeader().setReceiverId(senderId);
    }

    public RequestJsonMessage forwardMessage(RequestJsonMessage requestJsonMessage, Boolean isCreate){
        RequestMessage requestMessage = RequestMessage.builder().id(requestJsonMessage.getId())
                .header(requestJsonMessage.getHeader()).signature(requestJsonMessage.getSignature())
                .message(requestJsonMessage.getMessage().toString()).build();
        requestMessage.getHeader().setMessageType(MessageType.PROGRAM);
        requestMessage.getHeader().setAction(isCreate ? Action.CREATE : Action.UPDATE);
        StringBuilder url = new StringBuilder(requestMessage.getHeader().getReceiverId()).append("exchange/v1/program");
        restRepo.fetchResult(url, requestMessage);
        return requestJsonMessage;

    }

}
