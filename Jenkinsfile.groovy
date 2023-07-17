pipeline {
    agent any
    
    environment {
        ZOWE_OPT_HOST = '192.86.32.250'
        ZOWE_OPT_PORT = '10443'
    }

    tools {
        nodejs "Zowe CLI"
    }
    
    stages {
        stage('#0 Prepare_credentials') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'COBOL',
                            usernameVariable: 'USERN',
                            passwordVariable: 'PASSW'
                        )
                    ]) {
                        env.ZOWE_OPT_USER = "${USERN}"
                        env.ZOWE_OPT_PASSWORD = "${PASSW}"
                    }
                }  
                sh 'zowe daemon enable'
            }
        }

        stage('#1 FTP') {
            steps {
                script {
                    /*
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "Z90319.JCL(COMPILE)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
                    if (commandOutput != 'CC 0000') {
                        error "Compile failure ${commandOutput}"
                    */
                    echo 'FTP'
                    }
                }
            }
        }

        stage('#2 Compile_COB') {
            when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }
            steps {
                script {
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "Z90319.JCL(COMPILE)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
                    if (commandOutput != 'CC 0000') {
                        error "Compile failure ${commandOutput}"
                    }
                }
                //sh 'zowe zos-jobs submit data-set "Z90319.JCL(COMPILE)" --wfo --rff retcode --rft string --reject-unauthorized false'
            }
        }

        stage('#3 Run_COB') {
            /*when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }*/
            steps {
                script {
                    def commandOutput = sh(script: 'zowe zos-jobs submit data-set "Z90319.JCL(RUN)" \
                        --wfo --rff retcode --rft string --reject-unauthorized false', returnStdout: true).trim()
                    
                    if (commandOutput != 'CC 0000') {
                        error "Run failure ${commandOutput}"
                    }
                }
            }
        }
    }
}