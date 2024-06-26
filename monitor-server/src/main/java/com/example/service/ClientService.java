package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Client;
import com.example.entity.vo.request.ClientDetailsVO;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RenameNodeVO;
import com.example.entity.vo.request.RuntimeDetailsVO;
import com.example.entity.vo.response.ClientDetailsResponseVO;
import com.example.entity.vo.response.ClientPreviewVO;
import com.example.entity.vo.response.ClientSimpleVO;
import com.example.entity.vo.response.RuntimeHistoryVO;

import java.util.List;

public interface ClientService extends IService<Client> {
    boolean verifyAndRegister(String token);

    String registerToken();

    Client findClientById(int id);

    Client findClientByToken(String token);

    void updateClientDetail(ClientDetailsVO vo, Client client);

    List<ClientPreviewVO> listClients();

    void updateRuntimeDetails(RuntimeDetailsVO vo, Client client);

    void renameClient(RenameClientVO vo);

    void renameNode(RenameNodeVO vo);

    ClientDetailsResponseVO clientDetails(Integer clientId);

    RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId);

    RuntimeDetailsVO clientRuntimeDetailsNow(int clientId);

    void deleteClient(int clientId);
    List<ClientSimpleVO> listSimpleList();
}
