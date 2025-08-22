package com.enterprise.email.service.impl;

import com.enterprise.email.entity.Domain;
import com.enterprise.email.entity.Email;
import com.enterprise.email.entity.EmailAttachment;
import com.enterprise.email.entity.UserAlias;
import com.enterprise.email.entity.ImapPop3Config;
import com.enterprise.email.mapper.ImapPop3ConfigMapper;
import com.enterprise.email.service.ImapService;
import com.enterprise.email.utils.EmailContentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IMAP/POP3邮件接收服务实现类
 * 支持Dovecot服务器集成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImapServiceImpl implements ImapService {

    private final ImapPop3ConfigMapper imapPop3ConfigMapper;

    @Value("${app.email.imap.host:localhost}")
    private String defaultImapHost;

    @Value("${app.email.imap.port:143}")
    private Integer defaultImapPort;

    @Value("${app.email.imap.ssl:false}")
    private Boolean defaultImapSsl;

    @Value("${app.email.pop3.host:localhost}")
    private String defaultPop3Host;

    @Value("${app.email.pop3.port:110}")
    private Integer defaultPop3Port;

    @Value("${app.email.pop3.ssl:false}")
    private Boolean defaultPop3Ssl;

    @Value("${app.email.mailbox.path:/var/mail}")
    private String defaultMailboxPath;

    // 连接缓存
    private final Map<String, Store> storeCache = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    private Store store;
    private Folder currentFolder;
    private Session session;

    @Override
    public void connect(UserAlias alias, Domain domain) throws MessagingException {
        try {
            // 创建Session
            Properties props = new Properties();
            props.put("mail.store.protocol", "imap");
            props.put("mail.imap.host", domain.getImapHost());
            props.put("mail.imap.port", domain.getImapPort().toString());
            props.put("mail.imap.timeout", "30000");
            props.put("mail.imap.connectiontimeout", "30000");
            
            if (domain.getImapSsl()) {
                props.put("mail.imap.ssl.enable", "true");
                props.put("mail.imap.ssl.trust", domain.getImapHost());
            } else {
                props.put("mail.imap.starttls.enable", "true");
            }
            
            session = Session.getInstance(props);
            
            // 连接到IMAP服务器
            store = session.getStore("imap");
            // 这里需要配置IMAP用户名和密码
            store.connect(domain.getImapHost(), alias.getAliasAddress(), "password");
            
            log.info("IMAP连接成功: {}", alias.getAliasAddress());
            
        } catch (MessagingException e) {
            log.error("IMAP连接失败: {}, error: {}", alias.getAliasAddress(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (currentFolder != null && currentFolder.isOpen()) {
                currentFolder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (MessagingException e) {
            log.error("IMAP断开连接失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Email> syncEmails(UserAlias alias, String folderName) throws MessagingException {
        List<Email> emails = new ArrayList<>();
        
        try {
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            currentFolder = folder;
            
            // 获取所有邮件
            Message[] messages = folder.getMessages();
            
            for (Message message : messages) {
                try {
                    Email email = convertToEmail(message, alias);
                    if (email != null) {
                        emails.add(email);
                    }
                } catch (Exception e) {
                    log.error("转换邮件失败: {}", e.getMessage(), e);
                }
            }
            
            log.info("同步邮件完成: folder={}, count={}", folderName, emails.size());
            
        } catch (MessagingException e) {
            log.error("同步邮件失败: folder={}, error={}", folderName, e.getMessage());
            throw e;
        }
        
        return emails;
    }

    @Override
    public List<Email> getNewEmails(UserAlias alias, String folderName) throws MessagingException {
        List<Email> emails = new ArrayList<>();
        
        try {
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            currentFolder = folder;
            
            // 搜索未读邮件
            SearchTerm searchTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = folder.search(searchTerm);
            
            for (Message message : messages) {
                try {
                    Email email = convertToEmail(message, alias);
                    if (email != null) {
                        emails.add(email);
                    }
                } catch (Exception e) {
                    log.error("转换新邮件失败: {}", e.getMessage(), e);
                }
            }
            
            log.info("获取新邮件完成: folder={}, count={}", folderName, emails.size());
            
        } catch (MessagingException e) {
            log.error("获取新邮件失败: folder={}, error={}", folderName, e.getMessage());
            throw e;
        }
        
        return emails;
    }

    @Override
    public List<String> getFolders(UserAlias alias, Domain domain) throws MessagingException {
        List<String> folderNames = new ArrayList<>();
        
        try {
            if (!isConnected()) {
                connect(alias, domain);
            }
            
            Folder[] folders = store.getDefaultFolder().list("*");
            
            for (Folder folder : folders) {
                folderNames.add(folder.getName());
            }
            
            log.info("获取邮件夹列表完成: count={}", folderNames.size());
            
        } catch (MessagingException e) {
            log.error("获取邮件夹列表失败: {}", e.getMessage());
            throw e;
        }
        
        return folderNames;
    }

    @Override
    public void markAsRead(UserAlias alias, String messageUid) throws MessagingException {
        try {
            if (currentFolder == null || !currentFolder.isOpen()) {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_WRITE);
                currentFolder = folder;
            }
            
            // 根据UID查找邮件
            Message message = findMessageByUid(messageUid);
            if (message != null) {
                message.setFlag(Flags.Flag.SEEN, true);
                log.info("邮件已标记为已读: uid={}", messageUid);
            }
            
        } catch (MessagingException e) {
            log.error("标记邮件为已读失败: uid={}, error={}", messageUid, e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteEmail(UserAlias alias, String messageUid) throws MessagingException {
        try {
            if (currentFolder == null || !currentFolder.isOpen()) {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_WRITE);
                currentFolder = folder;
            }
            
            Message message = findMessageByUid(messageUid);
            if (message != null) {
                message.setFlag(Flags.Flag.DELETED, true);
                log.info("邮件已标记为删除: uid={}", messageUid);
            }
            
        } catch (MessagingException e) {
            log.error("删除邮件失败: uid={}, error={}", messageUid, e.getMessage());
            throw e;
        }
    }

    @Override
    public void moveEmail(UserAlias alias, String messageUid, String targetFolder) throws MessagingException {
        try {
            if (currentFolder == null || !currentFolder.isOpen()) {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_WRITE);
                currentFolder = folder;
            }
            
            Message message = findMessageByUid(messageUid);
            if (message != null) {
                Folder target = store.getFolder(targetFolder);
                if (!target.exists()) {
                    target.create(Folder.HOLDS_MESSAGES);
                }
                
                currentFolder.copyMessages(new Message[]{message}, target);
                message.setFlag(Flags.Flag.DELETED, true);
                
                log.info("邮件已移动: uid={}, target={}", messageUid, targetFolder);
            }
            
        } catch (MessagingException e) {
            log.error("移动邮件失败: uid={}, target={}, error={}", 
                     messageUid, targetFolder, e.getMessage());
            throw e;
        }
    }

    @Override
    public Email getEmailContent(UserAlias alias, String messageUid) throws MessagingException {
        try {
            Message message = findMessageByUid(messageUid);
            if (message != null) {
                return convertToEmail(message, alias);
            }
            return null;
            
        } catch (MessagingException e) {
            log.error("获取邮件内容失败: uid={}, error={}", messageUid, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isConnected() {
        return store != null && store.isConnected();
    }

    @Override
    public int getUnreadCount(UserAlias alias, String folderName) throws MessagingException {
        try {
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            
            int unreadCount = folder.getUnreadMessageCount();
            folder.close(false);
            
            return unreadCount;
            
        } catch (MessagingException e) {
            log.error("获取未读邮件数失败: folder={}, error={}", folderName, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<Email> searchEmails(UserAlias alias, String folderName, String searchTerm) throws MessagingException {
        List<Email> emails = new ArrayList<>();
        
        try {
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            
            // 构建搜索条件
            SearchTerm subjectTerm = new SubjectTerm(searchTerm);
            SearchTerm fromTerm = new FromStringTerm(searchTerm);
            SearchTerm contentTerm = new BodyTerm(searchTerm);
            
            SearchTerm orTerm = new OrTerm(new OrTerm(subjectTerm, fromTerm), contentTerm);
            
            Message[] messages = folder.search(orTerm);
            
            for (Message message : messages) {
                try {
                    Email email = convertToEmail(message, alias);
                    if (email != null) {
                        emails.add(email);
                    }
                } catch (Exception e) {
                    log.error("转换搜索邮件失败: {}", e.getMessage(), e);
                }
            }
            
            folder.close(false);
            
            log.info("搜索邮件完成: folder={}, term={}, count={}", 
                    folderName, searchTerm, emails.size());
            
        } catch (MessagingException e) {
            log.error("搜索邮件失败: folder={}, term={}, error={}", 
                     folderName, searchTerm, e.getMessage());
            throw e;
        }
        
        return emails;
    }

    /**
     * 将JavaMail Message转换为Email实体
     */
    private Email convertToEmail(Message message, UserAlias alias) throws MessagingException {
        try {
            Email email = new Email();
            
            // 基本信息
            email.setUserId(alias.getUserId());
            email.setAliasId(alias.getId());
            email.setMessageUid(String.valueOf(message.getMessageNumber()));
            
            // 设置Message-ID
            String[] messageIds = message.getHeader("Message-ID");
            if (messageIds != null && messageIds.length > 0) {
                email.setMessageId(messageIds[0]);
            }
            
            // 发件人
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                email.setSender(fromAddresses[0].toString());
            }
            
            // 收件人
            Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
            if (toAddresses != null && toAddresses.length > 0) {
                email.setRecipient(Arrays.toString(toAddresses));
            }
            
            // 抄送
            Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
            if (ccAddresses != null && ccAddresses.length > 0) {
                email.setCc(Arrays.toString(ccAddresses));
            }
            
            // 密送
            Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
            if (bccAddresses != null && bccAddresses.length > 0) {
                email.setBcc(Arrays.toString(bccAddresses));
            }
            
            // 主题
            String subject = message.getSubject();
            if (subject != null) {
                email.setSubject(MimeUtility.decodeText(subject));
            }
            
            // 时间
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                email.setSentTime(LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault()));
            }
            
            Date receivedDate = message.getReceivedDate();
            if (receivedDate != null) {
                email.setReceivedTime(LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault()));
            } else {
                email.setReceivedTime(LocalDateTime.now());
            }
            
            // 邮件大小
            email.setSizeBytes((long) message.getSize());
            
            // 读取状态
            email.setIsRead(message.isSet(Flags.Flag.SEEN));
            
            // 重要标记
            email.setIsImportant(message.isSet(Flags.Flag.FLAGGED));
            
            // 邮件类型
            email.setEmailType(Email.TYPE_INBOX);
            
            // 解析邮件内容
            EmailContentParser.ParseResult parseResult = EmailContentParser.parseContent(message);
            email.setContentText(parseResult.getTextContent());
            email.setContentHtml(parseResult.getHtmlContent());
            email.setHasAttachment(parseResult.hasAttachments());
            email.setAttachmentCount(parseResult.getAttachmentCount());
            
            // 邮件状态
            email.setStatus(Email.STATUS_PROCESSED);
            
            return email;
            
        } catch (Exception e) {
            log.error("转换邮件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ImapPop3Config configureImapPop3Server(String domain, Map<String, Object> config) {
        ImapPop3Config imapPop3Config = new ImapPop3Config();
        imapPop3Config.setDomain(domain);
        imapPop3Config.setImapHost((String) config.getOrDefault("imapHost", defaultImapHost));
        imapPop3Config.setImapPort((Integer) config.getOrDefault("imapPort", defaultImapPort));
        imapPop3Config.setImapSsl((Boolean) config.getOrDefault("imapSsl", defaultImapSsl));
        imapPop3Config.setPop3Host((String) config.getOrDefault("pop3Host", defaultPop3Host));
        imapPop3Config.setPop3Port((Integer) config.getOrDefault("pop3Port", defaultPop3Port));
        imapPop3Config.setPop3Ssl((Boolean) config.getOrDefault("pop3Ssl", defaultPop3Ssl));
        imapPop3Config.setMailboxPath((String) config.getOrDefault("mailboxPath", defaultMailboxPath));
        imapPop3Config.setMailboxFormat((String) config.getOrDefault("mailboxFormat", "Maildir"));
        imapPop3Config.setMaxConnections((Integer) config.getOrDefault("maxConnections", 20));
        imapPop3Config.setConnectionTimeout((Integer) config.getOrDefault("connectionTimeout", 30));
        imapPop3Config.setEnabled(true);
        imapPop3Config.setStatus("INACTIVE");
        
        // 保存配置JSON
        imapPop3Config.setConfigJson(convertMapToJson(config));
        
        imapPop3ConfigMapper.insert(imapPop3Config);
        
        // 测试连接
        boolean imapOk = testImapConnection(imapPop3Config);
        boolean pop3Ok = testPop3Connection(imapPop3Config);
        
        if (imapOk && pop3Ok) {
            imapPop3Config.setStatus("ACTIVE");
        } else if (imapOk || pop3Ok) {
            imapPop3Config.setStatus("PARTIAL");
        } else {
            imapPop3Config.setStatus("ERROR");
        }
        
        imapPop3ConfigMapper.updateById(imapPop3Config);
        
        log.info("IMAP/POP3服务器配置完成: domain={}, imap={}:{}, pop3={}:{}", 
                domain, imapPop3Config.getImapHost(), imapPop3Config.getImapPort(),
                imapPop3Config.getPop3Host(), imapPop3Config.getPop3Port());
        
        return imapPop3Config;
    }

    @Override
    public boolean testImapConnection(ImapPop3Config config) {
        try {
            Properties props = createImapProperties(config);
            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");
            
            // 测试连接
            store.connect(config.getImapHost(), "test", "test");
            store.close();
            
            // 更新测试结果
            config.setLastTestAt(LocalDateTime.now());
            config.setLastTestResult("IMAP: SUCCESS");
            
            log.info("IMAP连接测试成功: {}", config.getDomain());
            return true;
            
        } catch (Exception e) {
            log.error("IMAP连接测试失败: {}", e.getMessage(), e);
            
            // 更新测试结果
            config.setLastTestAt(LocalDateTime.now());
            config.setLastTestResult("IMAP: FAILED - " + e.getMessage());
            
            return false;
        }
    }

    @Override
    public boolean testPop3Connection(ImapPop3Config config) {
        try {
            Properties props = createPop3Properties(config);
            Session session = Session.getInstance(props);
            Store store = session.getStore("pop3");
            
            // 测试连接
            store.connect(config.getPop3Host(), "test", "test");
            store.close();
            
            // 更新测试结果
            String currentResult = config.getLastTestResult();
            String newResult = currentResult != null ? currentResult + "; POP3: SUCCESS" : "POP3: SUCCESS";
            config.setLastTestResult(newResult);
            
            log.info("POP3连接测试成功: {}", config.getDomain());
            return true;
            
        } catch (Exception e) {
            log.error("POP3连接测试失败: {}", e.getMessage(), e);
            
            // 更新测试结果
            String currentResult = config.getLastTestResult();
            String newResult = currentResult != null ? currentResult + "; POP3: FAILED" : "POP3: FAILED - " + e.getMessage();
            config.setLastTestResult(newResult);
            
            return false;
        }
    }

    @Override
    public ImapPop3Config getImapPop3ConfigByDomain(String domain) {
        return imapPop3ConfigMapper.selectByDomain(domain);
    }

    @Override
    public ImapPop3Config updateImapPop3Config(Long configId, Map<String, Object> config) {
        ImapPop3Config imapPop3Config = imapPop3ConfigMapper.selectById(configId);
        if (imapPop3Config == null) {
            throw new IllegalArgumentException("IMAP/POP3配置不存在: " + configId);
        }
        
        // 更新配置
        if (config.containsKey("imapHost")) {
            imapPop3Config.setImapHost((String) config.get("imapHost"));
        }
        if (config.containsKey("imapPort")) {
            imapPop3Config.setImapPort((Integer) config.get("imapPort"));
        }
        if (config.containsKey("imapSsl")) {
            imapPop3Config.setImapSsl((Boolean) config.get("imapSsl"));
        }
        if (config.containsKey("pop3Host")) {
            imapPop3Config.setPop3Host((String) config.get("pop3Host"));
        }
        if (config.containsKey("pop3Port")) {
            imapPop3Config.setPop3Port((Integer) config.get("pop3Port"));
        }
        if (config.containsKey("pop3Ssl")) {
            imapPop3Config.setPop3Ssl((Boolean) config.get("pop3Ssl"));
        }
        if (config.containsKey("enabled")) {
            imapPop3Config.setEnabled((Boolean) config.get("enabled"));
        }
        
        imapPop3Config.setConfigJson(convertMapToJson(config));
        imapPop3ConfigMapper.updateById(imapPop3Config);
        
        // 清除缓存
        storeCache.remove(imapPop3Config.getDomain());
        sessionCache.remove(imapPop3Config.getDomain());
        
        // 重新测试连接
        if (imapPop3Config.getEnabled()) {
            boolean imapOk = testImapConnection(imapPop3Config);
            boolean pop3Ok = testPop3Connection(imapPop3Config);
            
            if (imapOk && pop3Ok) {
                imapPop3Config.setStatus("ACTIVE");
            } else if (imapOk || pop3Ok) {
                imapPop3Config.setStatus("PARTIAL");
            } else {
                imapPop3Config.setStatus("ERROR");
            }
            imapPop3ConfigMapper.updateById(imapPop3Config);
        }
        
        return imapPop3Config;
    }

    @Override
    public boolean createUserMailbox(UserAlias alias) {
        try {
            String domain = extractDomain(alias.getAliasAddress());
            ImapPop3Config config = getImapPop3ConfigByDomain(domain);
            
            if (config == null || !config.getEnabled()) {
                log.warn("域名{}的IMAP/POP3配置不可用", domain);
                return false;
            }
            
            // 创建用户邮箱目录
            String mailboxPath = config.getMailboxPath() + "/" + alias.getAliasAddress();
            File mailboxDir = new File(mailboxPath);
            
            if ("Maildir".equals(config.getMailboxFormat())) {
                // Maildir格式
                File curDir = new File(mailboxDir, "cur");
                File newDir = new File(mailboxDir, "new");
                File tmpDir = new File(mailboxDir, "tmp");
                
                boolean created = curDir.mkdirs() && newDir.mkdirs() && tmpDir.mkdirs();
                if (created) {
                    log.info("Maildir邮箱创建成功: {}", alias.getAliasAddress());
                    return true;
                }
            } else {
                // mbox格式
                boolean created = mailboxDir.mkdirs();
                if (created) {
                    File mboxFile = new File(mailboxDir, "mbox");
                    mboxFile.createNewFile();
                    log.info("mbox邮箱创建成功: {}", alias.getAliasAddress());
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("创建用户邮箱失败: {}", alias.getAliasAddress(), e);
            return false;
        }
    }

    @Override
    public boolean deleteUserMailbox(UserAlias alias) {
        try {
            String domain = extractDomain(alias.getAliasAddress());
            ImapPop3Config config = getImapPop3ConfigByDomain(domain);
            
            if (config == null) {
                log.warn("域名{}的IMAP/POP3配置不存在", domain);
                return false;
            }
            
            // 删除用户邮箱目录
            String mailboxPath = config.getMailboxPath() + "/" + alias.getAliasAddress();
            File mailboxDir = new File(mailboxPath);
            
            if (mailboxDir.exists()) {
                boolean deleted = deleteDirectory(mailboxDir);
                if (deleted) {
                    log.info("用户邮箱删除成功: {}", alias.getAliasAddress());
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("删除用户邮箱失败: {}", alias.getAliasAddress(), e);
            return false;
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void syncAllUserEmails() {
        log.info("开始同步所有用户邮件");
        
        List<ImapPop3Config> configs = imapPop3ConfigMapper.selectEnabledConfigs();
        
        for (ImapPop3Config config : configs) {
            try {
                syncDomainEmails(config);
            } catch (Exception e) {
                log.error("同步域名{}邮件失败", config.getDomain(), e);
            }
        }
        
        log.info("所有用户邮件同步完成");
    }

    @Override
    public Map<String, Object> getMailboxStats(UserAlias alias) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            String domain = extractDomain(alias.getAliasAddress());
            ImapPop3Config config = getImapPop3ConfigByDomain(domain);
            
            if (config == null) {
                return stats;
            }
            
            String mailboxPath = config.getMailboxPath() + "/" + alias.getAliasAddress();
            File mailboxDir = new File(mailboxPath);
            
            if (mailboxDir.exists()) {
                long totalSize = calculateDirectorySize(mailboxDir);
                int messageCount = countMessages(mailboxDir, config.getMailboxFormat());
                
                stats.put("totalSize", totalSize);
                stats.put("messageCount", messageCount);
                stats.put("mailboxPath", mailboxPath);
                stats.put("mailboxFormat", config.getMailboxFormat());
                stats.put("lastModified", mailboxDir.lastModified());
            }
            
        } catch (Exception e) {
            log.error("获取邮箱统计失败: {}", alias.getAliasAddress(), e);
        }
        
        return stats;
    }

    @Override
    public long getFolderSize(UserAlias alias, String folderName) {
        try {
            String domain = extractDomain(alias.getAliasAddress());
            ImapPop3Config config = getImapPop3ConfigByDomain(domain);
            
            if (config == null) {
                return 0;
            }
            
            String folderPath = config.getMailboxPath() + "/" + alias.getAliasAddress() + "/" + folderName;
            File folderDir = new File(folderPath);
            
            if (folderDir.exists()) {
                return calculateDirectorySize(folderDir);
            }
            
        } catch (Exception e) {
            log.error("获取邮件夹大小失败: {} - {}", alias.getAliasAddress(), folderName, e);
        }
        
        return 0;
    }

    @Override
    public void compactFolder(UserAlias alias, String folderName) {
        try {
            if (currentFolder != null && currentFolder.isOpen()) {
                // 执行IMAP EXPUNGE命令来删除标记为删除的邮件
                currentFolder.expunge();
                log.info("邮件夹压缩完成: {} - {}", alias.getAliasAddress(), folderName);
            }
        } catch (Exception e) {
            log.error("压缩邮件夹失败: {} - {}", alias.getAliasAddress(), folderName, e);
        }
    }

    @Override
    public boolean toggleImapPop3Service(String domain, boolean enabled) {
        ImapPop3Config config = getImapPop3ConfigByDomain(domain);
        if (config != null) {
            config.setEnabled(enabled);
            config.setStatus(enabled ? "ACTIVE" : "INACTIVE");
            imapPop3ConfigMapper.updateById(config);
            
            if (!enabled) {
                // 清除缓存
                storeCache.remove(domain);
                sessionCache.remove(domain);
            }
            
            return true;
        }
        return false;
    }

    @Override
    public List<Email> getPop3Emails(UserAlias alias) throws MessagingException {
        List<Email> emails = new ArrayList<>();
        
        try {
            String domain = extractDomain(alias.getAliasAddress());
            Store pop3Store = getPop3Store(domain);
            
            if (pop3Store != null) {
                Folder folder = pop3Store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                
                Message[] messages = folder.getMessages();
                
                for (Message message : messages) {
                    try {
                        Email email = convertToEmail(message, alias);
                        if (email != null) {
                            emails.add(email);
                        }
                    } catch (Exception e) {
                        log.error("转换POP3邮件失败: {}", e.getMessage(), e);
                    }
                }
                
                folder.close(false);
            }
            
        } catch (Exception e) {
            log.error("获取POP3邮件失败: {}", alias.getAliasAddress(), e);
            throw new MessagingException("获取POP3邮件失败", e);
        }
        
        return emails;
    }

    @Override
    public void deletePop3Email(UserAlias alias, String messageId) throws MessagingException {
        try {
            String domain = extractDomain(alias.getAliasAddress());
            Store pop3Store = getPop3Store(domain);
            
            if (pop3Store != null) {
                Folder folder = pop3Store.getFolder("INBOX");
                folder.open(Folder.READ_WRITE);
                
                Message message = findMessageByUid(messageId);
                if (message != null) {
                    message.setFlag(Flags.Flag.DELETED, true);
                    log.info("POP3邮件已标记为删除: {}", messageId);
                }
                
                folder.close(true); // expunge deleted messages
            }
            
        } catch (Exception e) {
            log.error("删除POP3邮件失败: {} - {}", alias.getAliasAddress(), messageId, e);
            throw new MessagingException("删除POP3邮件失败", e);
        }
    }

    // 私有辅助方法

    private Properties createImapProperties(ImapPop3Config config) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", config.getImapHost());
        props.put("mail.imap.port", config.getImapPort().toString());
        props.put("mail.imap.timeout", (config.getConnectionTimeout() * 1000) + "");
        props.put("mail.imap.connectiontimeout", (config.getConnectionTimeout() * 1000) + "");
        
        if (config.getImapSsl()) {
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.imap.ssl.trust", config.getImapHost());
        } else {
            props.put("mail.imap.starttls.enable", "true");
        }
        
        return props;
    }

    private Properties createPop3Properties(ImapPop3Config config) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3");
        props.put("mail.pop3.host", config.getPop3Host());
        props.put("mail.pop3.port", config.getPop3Port().toString());
        props.put("mail.pop3.timeout", (config.getConnectionTimeout() * 1000) + "");
        props.put("mail.pop3.connectiontimeout", (config.getConnectionTimeout() * 1000) + "");
        
        if (config.getPop3Ssl()) {
            props.put("mail.pop3.ssl.enable", "true");
            props.put("mail.pop3.ssl.trust", config.getPop3Host());
        } else {
            props.put("mail.pop3.starttls.enable", "true");
        }
        
        return props;
    }

    private Store getPop3Store(String domain) throws MessagingException {
        ImapPop3Config config = getImapPop3ConfigByDomain(domain);
        if (config == null || !config.getEnabled()) {
            return null;
        }
        
        Properties props = createPop3Properties(config);
        Session session = Session.getInstance(props);
        Store store = session.getStore("pop3");
        
        // 这里需要实际的用户认证
        store.connect(config.getPop3Host(), "username", "password");
        
        return store;
    }

    private void syncDomainEmails(ImapPop3Config config) {
        // 实现域名下所有用户的邮件同步逻辑
        log.info("同步域名{}的邮件", config.getDomain());
        // 这里可以遍历该域名下的所有用户，逐个同步邮件
    }

    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } else {
            size = directory.length();
        }
        return size;
    }

    private int countMessages(File mailboxDir, String mailboxFormat) {
        if ("Maildir".equals(mailboxFormat)) {
            File curDir = new File(mailboxDir, "cur");
            File newDir = new File(mailboxDir, "new");
            
            int count = 0;
            if (curDir.exists()) {
                File[] curFiles = curDir.listFiles();
                count += curFiles != null ? curFiles.length : 0;
            }
            if (newDir.exists()) {
                File[] newFiles = newDir.listFiles();
                count += newFiles != null ? newFiles.length : 0;
            }
            return count;
        } else {
            // mbox格式需要解析文件内容来计算邮件数量
            // 这里简化处理，返回1如果文件存在
            File mboxFile = new File(mailboxDir, "mbox");
            return mboxFile.exists() && mboxFile.length() > 0 ? 1 : 0;
        }
    }

    private String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : "";
    }

    private String convertMapToJson(Map<String, Object> map) {
        // 简单的JSON转换，实际项目中应使用Jackson
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            json.append("\"").append(key).append("\":\"").append(value).append("\",");
        });
        if (json.length() > 1) {
            json.setLength(json.length() - 1); // 移除最后的逗号
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 根据UID查找邮件
     */
    private Message findMessageByUid(String messageUid) throws MessagingException {
        if (currentFolder == null || !currentFolder.isOpen()) {
            return null;
        }
        
        try {
            int messageNumber = Integer.parseInt(messageUid);
            return currentFolder.getMessage(messageNumber);
        } catch (NumberFormatException e) {
            log.error("无效的邮件UID: {}", messageUid);
            return null;
        }
    }
}