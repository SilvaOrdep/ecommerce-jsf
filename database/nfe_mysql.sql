-- =============================================
-- SISTEMA NF-E EMISSOR - MYSQL SCRIPT
-- Banco: MySQL 8.0+
-- Versão: 1.0
-- =============================================

-- Criar database
CREATE DATABASE nfedb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Usar o database
USE nfedb;

-- =============================================
-- TABELAS PRINCIPAIS
-- =============================================

-- Tabela de usuários
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    login VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL DEFAULT 'USER',
    ativo TINYINT(1) DEFAULT 1,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de empresa (apenas 1 registro)
CREATE TABLE empresa (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cnpj VARCHAR(14) UNIQUE NOT NULL,
    xnome VARCHAR(200) NOT NULL,
    xfant VARCHAR(200) NOT NULL,
    ie VARCHAR(20) NOT NULL,
    crt VARCHAR(1) NOT NULL DEFAULT '3',
    -- Endereço
    xlgr VARCHAR(200) NOT NULL,
    nro VARCHAR(10) NOT NULL,
    xcpl VARCHAR(100),
    xbairro VARCHAR(100) NOT NULL,
    cmun VARCHAR(7) NOT NULL,
    xmun VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    cpais VARCHAR(4) DEFAULT '1058',
    xpais VARCHAR(50) DEFAULT 'Brasil',
    fone VARCHAR(15),
    email VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de configurações (apenas 1 registro)
CREATE TABLE configuracao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    webservice_uf VARCHAR(2) NOT NULL DEFAULT 'BA',
    emitente_uf VARCHAR(2) NOT NULL DEFAULT 'BA',
    webservice_ambiente VARCHAR(1) NOT NULL DEFAULT '2',
    caminho_schemas VARCHAR(500) NOT NULL,
    caminho_certificado VARCHAR(500) NOT NULL,
    senha_certificado VARCHAR(100) NOT NULL,
    caminho_xml VARCHAR(500) NOT NULL,
    gerar_pdf VARCHAR(1) DEFAULT '1',
    porta_servidor VARCHAR(5) DEFAULT '3435',
    serie_nfe INT NOT NULL,
    numero_nfe INT NOT NULL,
    natureza_operacao VARCHAR(100) DEFAULT 'VENDA',
    finalidade_emissao VARCHAR(1) DEFAULT '1',
    consumidor_final VARCHAR(1) DEFAULT '1',
    presenca_comprador VARCHAR(1) DEFAULT '1',
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de produtos
CREATE TABLE produto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cprod VARCHAR(50) UNIQUE NOT NULL,
    cean VARCHAR(14),
    xprod VARCHAR(500) NOT NULL,
    ncm VARCHAR(8) NOT NULL,
    cest VARCHAR(7),
    cfop VARCHAR(4) NOT NULL,
    ucom VARCHAR(6) NOT NULL,
    qcom DECIMAL(15,4) DEFAULT 1.0000,
    vuncom DECIMAL(15,4) NOT NULL,
    vprod DECIMAL(15,2),
    ceantrib VARCHAR(14),
    utrib VARCHAR(6),
    qtrib DECIMAL(15,4) DEFAULT 1.0000,
    vuntrib DECIMAL(15,4),
    indtot VARCHAR(1) DEFAULT '1',
    -- Campos de impostos
    orig VARCHAR(1) DEFAULT '0',
    cst_icms VARCHAR(3) DEFAULT '00',
    modbc_icms VARCHAR(1) DEFAULT '0',
    picms DECIMAL(5,2) DEFAULT 7.00,
    cst_pis VARCHAR(2) DEFAULT '01',
    ppis DECIMAL(5,2) DEFAULT 1.65,
    cst_cofins VARCHAR(2) DEFAULT '01',
    pcofins DECIMAL(5,2) DEFAULT 7.60,
    ativo TINYINT(1) DEFAULT 1,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de clientes
CREATE TABLE cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    xnome VARCHAR(200) NOT NULL,
    cpf VARCHAR(11),
    cnpj VARCHAR(14),
    ie VARCHAR(20),
    indiedest VARCHAR(1) DEFAULT '9',
    -- Endereço
    xlgr VARCHAR(200) NOT NULL,
    nro VARCHAR(10) NOT NULL,
    xcpl VARCHAR(100),
    xbairro VARCHAR(100) NOT NULL,
    cmun VARCHAR(7) NOT NULL,
    xmun VARCHAR(100) NOT NULL,
    uf VARCHAR(2) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    fone VARCHAR(15),
    email VARCHAR(100),
    ativo TINYINT(1) DEFAULT 1,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de vendas
CREATE TABLE venda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT NOT NULL,
    data_venda TIMESTAMP NOT NULL,
    numero_nfe INT,
    chave_nfe VARCHAR(44),
    protocolo_nfe VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDENTE',
    valor_total DECIMAL(15,2) DEFAULT 0.00,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela de itens da venda
CREATE TABLE item_venda (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venda_id INT NOT NULL,
    produto_id INT NOT NULL,
    quantidade DECIMAL(15,4) NOT NULL DEFAULT 1.0000,
    valor_unitario DECIMAL(15,4) NOT NULL,
    valor_total DECIMAL(15,2) NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- CONSTRAINTS E RELACIONAMENTOS
-- =============================================

-- Constraints para venda
ALTER TABLE venda 
ADD CONSTRAINT fk_venda_cliente 
FOREIGN KEY (cliente_id) REFERENCES cliente(id);

-- Constraints para item_venda
ALTER TABLE item_venda 
ADD CONSTRAINT fk_item_venda_venda 
FOREIGN KEY (venda_id) REFERENCES venda(id);

ALTER TABLE item_venda 
ADD CONSTRAINT fk_item_venda_produto 
FOREIGN KEY (produto_id) REFERENCES produto(id);

-- =============================================
-- ÍNDICES PARA PERFORMANCE
-- =============================================

-- Índices para produtos
CREATE INDEX idx_produto_cprod ON produto(cprod);
CREATE INDEX idx_produto_ativo ON produto(ativo);
CREATE INDEX idx_produto_ncm ON produto(ncm);

-- Índices para clientes
CREATE INDEX idx_cliente_documento ON cliente(cpf, cnpj);
CREATE INDEX idx_cliente_nome ON cliente(xnome);
CREATE INDEX idx_cliente_ativo ON cliente(ativo);

-- Índices para vendas
CREATE INDEX idx_venda_cliente ON venda(cliente_id);
CREATE INDEX idx_venda_data ON venda(data_venda);
CREATE INDEX idx_venda_status ON venda(status);
CREATE INDEX idx_venda_numero_nfe ON venda(numero_nfe);
CREATE INDEX idx_venda_chave_nfe ON venda(chave_nfe);

-- Índices para itens de venda
CREATE INDEX idx_item_venda_venda ON item_venda(venda_id);
CREATE INDEX idx_item_venda_produto ON item_venda(produto_id);

-- Índices para usuários
CREATE INDEX idx_usuario_login ON usuario(login);
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_ativo ON usuario(ativo);

-- =============================================
-- DADOS INICIAIS
-- =============================================

-- Inserir usuário administrador padrão
INSERT INTO usuario (nome, login, email, senha, perfil, ativo) 
VALUES ('Administrador', 'admin', 'admin@empresa.com', 'admin123', 'ADMIN', 1);

-- Inserir configuração padrão
INSERT INTO configuracao (
    webservice_uf, emitente_uf, webservice_ambiente, 
    caminho_schemas, caminho_certificado, senha_certificado, 
    caminho_xml, gerar_pdf, porta_servidor, serie_nfe, 
    natureza_operacao, finalidade_emissao, consumidor_final, presenca_comprador
) VALUES (
    'BA', 'BA', '2', 
    '/home/schemas/', '/home/certificados/certificado.pfx', '123456', 
    '/home/xml', '1', '3435', '1', 
    'VENDA', '1', '1', '1'
);

-- Inserir empresa padrão (dados fictícios)
INSERT INTO empresa (
    cnpj, xnome, xfant, ie, crt, 
    xlgr, nro, xcpl, xbairro, cmun, xmun, uf, cep, fone, email
) VALUES (
    '33630582000149', 
    'Editora Roberto Luiz Souza Monteiro', 
    'Editora Science Books', 
    '158039716', '3', 
    'Rua Chile', 's/n', 'Edifício Eduardo De Moraes, sala 606', 'Centro', '2927408', 'Salvador', 'BA', 
    '40020000', '71991721874', 'roberto@souzamonteiro.com'
);

-- Inserir alguns produtos de exemplo
INSERT INTO produto (
    cprod, cean, xprod, ncm, cfop, ucom, vuncom,
    cest, orig, cst_icms, picms, cst_pis, ppis, cst_cofins, pcofins
) VALUES 
(
    '7898480650104', '7898480650104',
    'Óleo de cambio manual SAE 80W Flex Oil 1Ltl', 
    '27101932', '5102', 'UN', 13.00,
    '0600500', '0', '00', 7.00, '01', 1.65, '01', 7.60
),
(
    '7890647097098', '7890647097098',
    'Caneta esferográfica azul', 
    '96081000', '5102', 'UN', 1.49,
    '1902700', '0', '00', 18.50, '01', 1.65, '01', 7.60
);

-- Inserir alguns clientes de exemplo
INSERT INTO cliente (
    xnome, cpf, xlgr, nro, xcpl, xbairro, cmun, xmun, uf, cep, fone, email
) VALUES 
(
    'Andréia Rita da Silva', 
    '40325635862',
    'Rua Tanhaçu', '11', 'Quadra 5', 'Ipitanga', '2919207', 'Lauro de Freitas', 'BA', 
    '42706730', '71991965292', 'andreiarita2@gmail.com'
);

-- =============================================
-- TRIGGERS MYSQL
-- =============================================

DELIMITER //

-- Trigger para calcular valor_produto
CREATE TRIGGER trigger_calcular_valor_produto
BEFORE INSERT ON produto
FOR EACH ROW
BEGIN
    -- Calcular vprod baseado em vuncom e qcom
    IF NEW.vuncom IS NOT NULL THEN
        SET NEW.vprod = NEW.vuncom * COALESCE(NEW.qcom, 1.0000);
    END IF;
    
    -- Se vuntrib não for informado, usar vuncom
    IF NEW.vuntrib IS NULL AND NEW.vuncom IS NOT NULL THEN
        SET NEW.vuntrib = NEW.vuncom;
    END IF;
END//

-- Trigger para calcular valor_item_venda
CREATE TRIGGER trigger_calcular_valor_item_venda
BEFORE INSERT ON item_venda
FOR EACH ROW
BEGIN
    SET NEW.valor_total = NEW.quantidade * NEW.valor_unitario;
END//

DELIMITER ;

-- =============================================
-- VIEWS ÚTEIS
-- =============================================

-- View para relatório de produtos
CREATE VIEW vw_produtos_ativos AS
SELECT 
    id,
    cprod,
    xprod,
    ncm,
    vuncom,
    ativo
FROM produto 
WHERE ativo = 1
ORDER BY xprod;

-- View para relatório de clientes
CREATE VIEW vw_clientes_ativos AS
SELECT 
    id,
    xnome,
    COALESCE(cpf, cnpj) as documento,
    xmun,
    uf,
    ativo
FROM cliente 
WHERE ativo = 1
ORDER BY xnome;

-- View para relatório de vendas
CREATE VIEW vw_vendas_completas AS
SELECT 
    v.id,
    v.data_venda,
    v.numero_nfe,
    v.chave_nfe,
    v.status,
    v.valor_total,
    c.xnome as cliente_nome,
    c.cpf as cliente_cpf,
    c.cnpj as cliente_cnpj
FROM venda v
JOIN cliente c ON v.cliente_id = c.id
ORDER BY v.data_venda DESC;

-- View para itens de venda com detalhes do produto
CREATE VIEW vw_itens_venda_detalhados AS
SELECT 
    iv.id,
    iv.venda_id,
    iv.produto_id,
    p.cprod,
    p.xprod,
    p.ncm,
    iv.quantidade,
    iv.valor_unitario,
    iv.valor_total
FROM item_venda iv
JOIN produto p ON iv.produto_id = p.id;

-- =============================================
-- PERMISSÕES
-- =============================================

-- Criar usuário específico (opcional)
CREATE USER 'nfe_user'@'localhost' IDENTIFIED BY 'nfe123';
GRANT ALL PRIVILEGES ON nfedb.* TO 'nfe_user'@'localhost';
FLUSH PRIVILEGES;

-- =============================================
-- VALIDAÇÕES FINAIS
-- =============================================

-- Verificar se as tabelas foram criadas
SHOW TABLES;

-- Contar registros iniciais
SELECT 'usuario' as tabela, COUNT(*) as total FROM usuario
UNION ALL
SELECT 'empresa', COUNT(*) FROM empresa
UNION ALL
SELECT 'configuracao', COUNT(*) FROM configuracao
UNION ALL
SELECT 'produto', COUNT(*) FROM produto
UNION ALL
SELECT 'cliente', COUNT(*) FROM cliente;

-- =============================================
-- MENSAGEM FINAL
-- =============================================

SELECT '=========================================' as '';
SELECT 'BANCO NF-E EMISSOR CRIADO COM SUCESSO!' as '';
SELECT '=========================================' as '';
SELECT 'Database: nfedb' as '';
SELECT 'Usuário padrão: admin / admin123' as '';
SELECT '=========================================' as '';