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
        stage('Prepare_credentials') {
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

        stage('Compile_COB') {
            when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }
            steps {
                sh 'zowe zos-jobs submit data-set "Z90319.JCL(COMPILE)" --wfo --rff retcode --rft string --reject-unauthorized false'
            }
        }

        stage('Run_COB') {
            when {
                expression {currentBuild.currentResult == 'SUCCESS'}
            }
            steps {
                sh 'zowe zos-jobs submit data-set "Z90319.JCL(RUN)" --wfo --rff retcode --rft string --reject-unauthorized false'
            }
        }
    }
}