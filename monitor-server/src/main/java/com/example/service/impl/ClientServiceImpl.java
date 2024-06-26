package com.example.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetails;
import com.example.entity.vo.request.ClientDetailsVO;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RenameNodeVO;
import com.example.entity.vo.request.RuntimeDetailsVO;
import com.example.entity.vo.response.ClientDetailsResponseVO;
import com.example.entity.vo.response.ClientPreviewVO;
import com.example.entity.vo.response.ClientSimpleVO;
import com.example.entity.vo.response.RuntimeHistoryVO;
import com.example.mapper.ClientDetailsMapper;
import com.example.mapper.ClientMapper;
import com.example.service.ClientService;
import com.example.utils.InfluxDBUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken();

    //并发
    private final Map<Integer, Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String, Client> clientTokenCache = new ConcurrentHashMap<>();

    @Resource
    ClientDetailsMapper clientDetailsMapper;
    @Resource
    InfluxDBUtils influx;

    @PostConstruct
    public void initClientCache() {
        clientIdCache.clear();
        clientTokenCache.clear();
        this.list().forEach(this::addClientCache);
    }

    @Override
    public boolean verifyAndRegister(String token) {
        if (this.registerToken.equals(token)) {
            int id = this.randomClientId();
            Client client = new Client(id, "未命名主机", token, "cn", "未命名节点", new Date());
            if (this.save(client)) {
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    @Override
    public String registerToken() {
        return registerToken;
    }

    @Override
    public Client findClientById(int id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }

    @Override
    public void updateClientDetail(ClientDetailsVO vo, Client client) {
        ClientDetails details = new ClientDetails();
        BeanUtils.copyProperties(vo, details);
        details.setId(client.getId());
        if (Objects.nonNull(clientDetailsMapper.selectById(client.getId()))) {
            clientDetailsMapper.updateById(details);
        } else {
            clientDetailsMapper.insert(details);
        }
    }

    @Override
    public List<ClientPreviewVO> listClients() {

        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO vo = client.asViewObject(ClientPreviewVO.class);
            BeanUtils.copyProperties(clientDetailsMapper.selectById(vo.getId()), vo);
            RuntimeDetailsVO runtime = currentRuntime.get(client.getId());
            if (this.isOnline(runtime)) {
                BeanUtils.copyProperties(runtime, vo);
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }

    private boolean isOnline(RuntimeDetailsVO runtime) {
        return runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000;
    }

    private Map<Integer, RuntimeDetailsVO> currentRuntime = new ConcurrentHashMap<>();


    @Override
    public void updateRuntimeDetails(RuntimeDetailsVO vo, Client client) {
        currentRuntime.put(client.getId(), vo);
        influx.writeRuntimeData(client.getId(), vo);
    }

    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId()).set("name", vo.getName()));
        this.initClientCache();
    }

    @Override
    public void renameNode(RenameNodeVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId())
                .set("node", vo.getNode()).set("location", vo.getLocation()));
        this.initClientCache();
    }

    @Override
    public ClientDetailsResponseVO clientDetails(Integer clientId) {
        ClientDetailsResponseVO vo = this.clientIdCache.get(clientId).asViewObject(ClientDetailsResponseVO.class);
        BeanUtils.copyProperties(clientDetailsMapper.selectById(clientId), vo);
        vo.setOnline(this.isOnline(currentRuntime.get(clientId)));
        return vo;
    }

    @Override
    public RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId) {
        RuntimeHistoryVO vo = influx.readRuntimeData(clientId);
        ClientDetails details = clientDetailsMapper.selectById(clientId);
        BeanUtils.copyProperties(details, vo);
        return vo;
    }

    @Override
    public RuntimeDetailsVO clientRuntimeDetailsNow(int clientId) {
        return currentRuntime.get(clientId);
    }

    @Override
    public void deleteClient(int clientId) {
        this.removeById(clientId);
        clientDetailsMapper.deleteById(clientId);
        this.initClientCache();
        currentRuntime.remove(clientId);
    }

    @Override
    public List<ClientSimpleVO> listSimpleList() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO vo = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(clientDetailsMapper.selectById(vo.getId()), vo);
            return vo;
        }).toList();
    }


    private void addClientCache(Client client) {
        clientIdCache.put(client.getId(), client);
        clientTokenCache.put(client.getToken(), client);
    }

    private int randomClientId() {
        return new Random().nextInt(90000000) + 10000000;
    }

    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++)
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        System.out.println(sb);
        return sb.toString();
    }


}
