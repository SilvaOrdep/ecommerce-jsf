-- =============================================
-- SISTEMA NF-E EMISSOR - SCRIPT DE CRIAÇÃO DO BANCO
-- Banco: PostgreSQL
-- Versão: 1.0
-- =============================================

-- Criar database
CREATE DATABASE nfedb;

-- Conectar ao database
\c nfedb;

-- Criar usuário específico (opcional)
CREATE USER nfe_user WITH PASSWORD 'nfe123';
GRANT ALL PRIVILEGES ON DATABASE nfedb TO nfe_user;

-- =============================================
-- TABELAS PRINCIPAIS
-- =============================================

-- Tabela de usuários
CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    login VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL DEFAULT 'USER',
    ativo BOOLEAN DEFAULT true,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de empresa (apenas 1 registro)
CREATE TABLE empresa (
    id SERIAL PRIMARY KEY,
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
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de configurações (apenas 1 registro)
CREATE TABLE configuracao (
    id SERIAL PRIMARY KEY,
    webservice_uf VARCHAR(2) NOT NULL DEFAULT 'BA',
    emitente_uf VARCHAR(2) NOT NULL DEFAULT 'BA',
    webservice_ambiente VARCHAR(1) NOT NULL DEFAULT '2',
    caminho_schemas VARCHAR(500) NOT NULL,
    caminho_certificado VARCHAR(500) NOT NULL,
    senha_certificado VARCHAR(100) NOT NULL,
    caminho_xml VARCHAR(500) NOT NULL,
    gerar_pdf VARCHAR(1) DEFAULT '1',
    porta_servidor VARCHAR(5) DEFAULT '3435',
    serie_nfe VARCHAR(3) DEFAULT '1',
    natureza_operacao VARCHAR(100) DEFAULT 'VENDA',
    finalidade_emissao VARCHAR(1) DEFAULT '1',
    consumidor_final VARCHAR(1) DEFAULT '1',
    presenca_comprador VARCHAR(1) DEFAULT '1',
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de produtos
CREATE TABLE produto (
    id SERIAL PRIMARY KEY,
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
    ativo BOOLEAN DEFAULT true,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de clientes
CREATE TABLE cliente (
    id SERIAL PRIMARY KEY,
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
    ativo BOOLEAN DEFAULT true,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de vendas
CREATE TABLE venda (
    id SERIAL PRIMARY KEY,
    cliente_id INTEGER NOT NULL,
    data_venda TIMESTAMP NOT NULL,
    numero_nfe INTEGER,
    chave_nfe VARCHAR(44),
    protocolo_nfe VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDENTE',
    valor_total DECIMAL(15,2) DEFAULT 0.00,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de itens da venda
CREATE TABLE item_venda (
    id SERIAL PRIMARY KEY,
    venda_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
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
VALUES ('Administrador', 'admin', 'admin@empresa.com', 'admin123', 'ADMIN', true);

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
    xlgr, nro, xbairro, cmun, xmun, uf, cep, fone, email
) VALUES (
    '33630582000149', 
    'Editora Roberto LIEuiz Souza Monteiro', 
    'Editora Monteiro', 
    '158039716', '3', 
    'Rua Chile', 's/n', 'Centro', '2927408', 'Salvador', 'BA', 
    '40020000', '71991721874', 'contato@editoramonteiro.com'
);

-- Inserir alguns produtos de exemplo
INSERT INTO produto (
    cprod, xprod, ncm, cfop, ucom, vuncom,
    cest, orig, cst_icms, picms, cst_pis, ppis, cst_cofins, pcofins
) VALUES 
(
    '7898480650104', 
    'Livro - Gestão Empresarial', 
    '49019900', '5102', 'UN', 50.00,
    NULL, '0', '00', 0.00, '01', 1.65, '01', 7.60
),
(
    '7891234567890', 
    'Mouse Óptico USB', 
    '84716052', '5102', 'UN', 25.00,
    '0600500', '0', '00', 12.00, '01', 1.65, '01', 7.60
),
(
    '7899876543210', 
    'Teclado Mecânico', 
    '84716072', '5102', 'UN', 120.00,
    '0600500', '0', '00', 12.00, '01', 1.65, '01', 7.60
);

-- Inserir alguns clientes de exemplo
INSERT INTO cliente (
    xnome, cpf, xlgr, nro, xbairro, cmun, xmun, uf, cep, fone, email
) VALUES 
(
    'João Silva Santos', 
    '40325635862',
    'Rua Tanhaçu', '11', 'Ipitanga', '2919207', 'Lauro de Freitas', 'BA', 
    '42706730', '71991965292', 'joao.silva@email.com'
),
(
    'Maria Oliveira Souza', 
    '98765432100',
    'Av. Sete de Setembro', '100', 'Centro', '2927408', 'Salvador', 'BA', 
    '40020000', '7133334444', 'maria.oliveira@email.com'
);
INSERT INTO cliente (
    xnome, cnpj, xlgr, nro, xbairro, cmun, xmun, uf, cep, fone, email
) VALUES 
(
    'Tech Solutions LTDA', 
    '13937073000156',
    'Rua da Paz', '45', 'Pituba', '2927408', 'Salvador', 'BA', 
    '41830600', '7132223333', 'contato@techsolutions.com'
);

-- =============================================
-- FUNÇÕES E TRIGGERS
-- =============================================

-- Função para atualizar data_atualizacao
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.data_atualizacao = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers para atualização automática de data_atualizacao
CREATE TRIGGER update_usuario_updated_at 
    BEFORE UPDATE ON usuario 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_empresa_updated_at 
    BEFORE UPDATE ON empresa 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_configuracao_updated_at 
    BEFORE UPDATE ON configuracao 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_produto_updated_at 
    BEFORE UPDATE ON produto 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cliente_updated_at 
    BEFORE UPDATE ON cliente 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_venda_updated_at 
    BEFORE UPDATE ON venda 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Função para calcular valor_total do produto
CREATE OR REPLACE FUNCTION calcular_valor_produto()
RETURNS TRIGGER AS $$
BEGIN
    -- Calcular vprod baseado em vuncom e qcom
    IF NEW.vuncom IS NOT NULL THEN
        NEW.vprod = NEW.vuncom * COALESCE(NEW.qcom, 1.0000);
    END IF;
    
    -- Se vuntrib não for informado, usar vuncom
    IF NEW.vuntrib IS NULL AND NEW.vuncom IS NOT NULL THEN
        NEW.vuntrib = NEW.vuncom;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_calcular_valor_produto
    BEFORE INSERT OR UPDATE ON produto
    FOR EACH ROW EXECUTE FUNCTION calcular_valor_produto();

-- Função para calcular valor_total do item_venda
CREATE OR REPLACE FUNCTION calcular_valor_item_venda()
RETURNS TRIGGER AS $$
BEGIN
    NEW.valor_total = NEW.quantidade * NEW.valor_unitario;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_calcular_valor_item_venda
    BEFORE INSERT OR UPDATE ON item_venda
    FOR EACH ROW EXECUTE FUNCTION calcular_valor_item_venda();

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
WHERE ativo = true
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
WHERE ativo = true
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

-- Conceder permissões para o usuário nfe_user (se criado)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO nfe_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO nfe_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO nfe_user;

-- =============================================
-- VALIDAÇÕES FINAIS
-- =============================================

-- Verificar se as tabelas foram criadas
SELECT 
    table_name,
    table_type
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Contar registros iniciais
SELECT 
    'usuario' as tabela, COUNT(*) as total FROM usuario
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

DO $$
BEGIN
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'BANCO NF-E EMISSOR CRIADO COM SUCESSO!';
    RAISE NOTICE '=========================================';
    RAISE NOTICE 'Database: nfedb';
    RAISE NOTICE 'Usuário padrão: admin / admin123';
    RAISE NOTICE '=========================================';
END $$;