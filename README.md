# Synapticloop Panl default template project

This is an empty template project, this file explains how to use this 
starter project template to index content using the SolrJ 
connector.

Dependencies included are:

- Apache Tika, for a wide range of supported files and filetypes

**NOTE:** This is based on the Apache Solr version 9.8.0

## Step 1: Download The Solr Server, Extract, And Copy Config

> **NOTE:** You may skip this section if you have an existing Solr 
> installation that you wish to use.

Download the latest version from [https://solr.apache.org/downloads.html](https://solr.apache.org/downloads.html) 
(this example is using the Solr `9.8.0-slim` version) and extract it to the 
`./servers` directory.

Copy the Solr configuration `./servers/solr-9.8.0-slim/server/solr/configsets/_default/conf/` 
directory to `./config/solr/` 

You could try:

```shell
curl "https://archive.apache.org/dist/solr/solr/9.8.0/solr-9.8.0-slim.tgz" -o servers/solr-9.8.0-slim.tgz
tar xvzf servers/solr-9.8.0-slim.tgz -C servers
cp -r ./servers/solr-9.8.0-slim/server/solr/configsets/_default/conf/* ./config/solr/
```

## Step 2: Download the Panl Server and Extract The Files

Download the latest version from [https://github.com/synapticloop/panl/releases/](https://github.com/synapticloop/panl/releases/) 
(this example is using the Panl `9-2.0.0` version) and extract it to the .
`/servers` directory

You could try:

```shell
curl -L "https://github.com/synapticloop/panl/releases/download/2.0.0/solr-panl-9-2.0.0.tar" -o servers/solr-panl-9.2.0.0.tar
tar xvf servers/solr-panl-9.2.0.0.tar -C servers
```

## Step 3: Edit the `managed-schema.xml` file

Edit the file for your specific requirements

## Step 4: Generate the `panl.properties` files

You could try:

```shell
servers/solr-panl-9-2.0.0/bin/panl generate -properties config/panl/panl.properties -schema config/solr/managed-schema.xml -overwrite true
```

You will then be asked some questions.

## Step 5: Start The Solr Server

### 1. Start Solr

```shell
servers/solr-9.8.0-slim/bin/solr start -e cloud --no-prompt
```

### 2. Upload the Configuration

```shell
servers/solr-9.8.0-slim/bin/solr create -c YOUR_COLLETCION_NAME_HERE -d config/solr --shards 2 -rf 2
```

## Step 6: Index The Data

Use this template Main file to index the data

## Step 7: Start The Panl Server

```shell
 servers/solr-panl-9-2.0.0/bin/panl server -properties config/panl/panl.properties
```
